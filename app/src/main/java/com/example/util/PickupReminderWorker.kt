package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.model.RepairStage
import java.util.concurrent.TimeUnit

/** Reminds the shop to message customers whose device is ready and who have not been told yet. */
class PickupReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            val jobs = AppDatabase.getInstance(context).jobCardDao().getAllJobsSync()
            val now = System.currentTimeMillis()
            jobs.filter { it.stage == RepairStage.READY_FOR_PICKUP }.forEach { job ->
                if (PickupReminders.isSent(context, job)) return@forEach
                if (now < PickupReminders.readyAt(job) + PickupReminders.GRACE_MS) return@forEach
                if (now - PickupReminders.lastNotified(context, job) < PickupReminders.RENOTIFY_MS) return@forEach
                NotificationHelper.notifyPickupReminder(
                    context = context,
                    jobId = job.id,
                    jobNumber = job.jobNumber,
                    customerName = job.customerName,
                    deviceName = "${job.deviceBrand} ${job.deviceModel}".trim(),
                    deadline = PickupReminders.formatDeadline(context, job)
                )
                PickupReminders.markNotified(context, job)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "pickup_reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PickupReminderWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
