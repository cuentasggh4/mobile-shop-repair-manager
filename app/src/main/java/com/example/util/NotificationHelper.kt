package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.RepairStage

object NotificationHelper {

    const val CHANNEL_REPAIR_UPDATES = "repair_updates_channel"
    const val CHANNEL_BACKUP_SYNC = "backup_sync_channel"
    const val EXTRA_OPEN_JOB_ID = "extra_open_job_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val repairChannel = NotificationChannel(
            CHANNEL_REPAIR_UPDATES,
            context.getString(R.string.notif_channel_repairs_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_repairs_desc)
        }

        val backupChannel = NotificationChannel(
            CHANNEL_BACKUP_SYNC,
            context.getString(R.string.notif_channel_backup_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_backup_desc)
        }

        manager.createNotificationChannel(repairChannel)
        manager.createNotificationChannel(backupChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyJobCreated(
        context: Context,
        jobId: Long,
        jobNumber: String,
        customerName: String,
        deviceName: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val title = context.getString(R.string.notif_job_created_title, jobNumber)
        val body = context.getString(R.string.notif_job_created_body, customerName, deviceName)
        postNotification(
            context = context,
            channelId = CHANNEL_REPAIR_UPDATES,
            notificationId = (jobId.toInt() and 0x7FFFFFFF),
            title = title,
            body = body,
            jobId = jobId
        )
    }

    fun notifyStageUpdated(
        context: Context,
        jobId: Long,
        jobNumber: String,
        customerName: String,
        deviceName: String,
        stage: RepairStage
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val stageLabel = when (stage) {
            RepairStage.DIAGNOSTIC -> context.getString(R.string.status_diagnostics)
            RepairStage.WAITING_FOR_PARTS -> context.getString(R.string.status_waiting_parts)
            RepairStage.IN_PROGRESS -> context.getString(R.string.status_in_progress)
            RepairStage.READY_FOR_PICKUP -> context.getString(R.string.status_ready_deliver)
            RepairStage.DELIVERED -> context.getString(R.string.status_delivered)
            RepairStage.CANCELLED -> context.getString(R.string.status_cancelled)
        }

        val title = if (stage == RepairStage.READY_FOR_PICKUP) {
            context.getString(R.string.notif_ready_pickup_title, jobNumber)
        } else {
            context.getString(R.string.notif_stage_updated_title, jobNumber, stageLabel)
        }
        val body = context.getString(R.string.notif_stage_updated_body, customerName, deviceName, stageLabel)

        postNotification(
            context = context,
            channelId = CHANNEL_REPAIR_UPDATES,
            notificationId = ((jobId.toInt() + stage.ordinal * 1000) and 0x7FFFFFFF),
            title = title,
            body = body,
            jobId = jobId
        )
    }

    fun notifyBackupResult(
        context: Context,
        success: Boolean,
        detail: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val title = if (success) {
            context.getString(R.string.notif_backup_success_title)
        } else {
            context.getString(R.string.notif_backup_failed_title)
        }

        postNotification(
            context = context,
            channelId = CHANNEL_BACKUP_SYNC,
            notificationId = 9001,
            title = title,
            body = detail,
            jobId = null
        )
    }

    fun sendTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        postNotification(
            context = context,
            channelId = CHANNEL_REPAIR_UPDATES,
            notificationId = 9999,
            title = context.getString(R.string.notif_test_title),
            body = context.getString(R.string.notif_test_body),
            jobId = null
        )
        return true
    }

    fun notifyAppUpdateAvailable(
        context: Context,
        latestVersion: String,
        releaseUrl: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9002,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REPAIR_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_update_available_title, latestVersion))
            .setContentText(context.getString(R.string.notif_update_available_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_update_available_body))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9002, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        jobId: Long?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (jobId != null) {
                putExtra(EXTRA_OPEN_JOB_ID, jobId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }
}
