package com.example.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NightlyBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val syncManager = GoogleSheetsSyncManager(
                context = applicationContext,
                jobCardDao = db.jobCardDao(),
                shopProfileDao = db.shopProfileDao()
            )
            val config = syncManager.syncConfig.first()
            if (!config.isConnected || !config.nightlyBackupEnabled) {
                return Result.success()
            }

            val token = syncManager.getCachedAccessTokenOrNull()
                ?: return Result.retry()

            val backupName = syncManager.createDatedFullBackup(token, forceNewSnapshot = false)
            val notificationsEnabled = AppPreferences(applicationContext).notificationsEnabled.first()
            if (notificationsEnabled && !backupName.isNullOrBlank()) {
                NotificationHelper.notifyBackupResult(
                    context = applicationContext,
                    success = true,
                    detail = backupName
                )
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                val notificationsEnabled = runCatching {
                    AppPreferences(applicationContext).notificationsEnabled.first()
                }.getOrDefault(false)
                if (notificationsEnabled) {
                    NotificationHelper.notifyBackupResult(
                        context = applicationContext,
                        success = false,
                        detail = e.message ?: "Backup failed"
                    )
                }
                Result.failure()
            }
        }
    }
}

object BackupScheduler {
    private const val NIGHTLY_WORK_NAME = "nightly_google_drive_backup"

    fun scheduleNightlyBackup(context: Context) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NightlyBackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            NIGHTLY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelNightlyBackup(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(NIGHTLY_WORK_NAME)
    }
}
