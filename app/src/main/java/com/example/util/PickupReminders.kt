package com.example.util

import android.content.Context
import com.example.data.entity.JobCardEntity
import com.example.data.model.RepairStage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pickup notice settings and bookkeeping: when a device is ready, the shop is reminded to message the
 * customer with the pickup deadline until the message has been sent.
 */
object PickupReminders {
    const val DEFAULT_DEADLINE_DAYS = 15
    private const val DAY_MS = 24L * 60L * 60L * 1000L
    const val GRACE_MS = 3L * 60L * 60L * 1000L
    const val RENOTIFY_MS = 20L * 60L * 60L * 1000L

    private const val PREFS = "pickup_prefs"
    private const val KEY_DEADLINE_DAYS = "deadline_days"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getDeadlineDays(context: Context): Int =
        prefs(context).getInt(KEY_DEADLINE_DAYS, DEFAULT_DEADLINE_DAYS)

    fun setDeadlineDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_DEADLINE_DAYS, days.coerceIn(1, 365)).apply()
    }

    /** When the job last became ready for pickup. */
    fun readyAt(job: JobCardEntity): Long =
        job.statusLogs.lastOrNull { it.stage == RepairStage.READY_FOR_PICKUP }?.timestamp ?: job.updatedAt

    fun deadlineMillis(context: Context, job: JobCardEntity): Long =
        readyAt(job) + getDeadlineDays(context) * DAY_MS

    fun formatDeadline(context: Context, job: JobCardEntity): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(deadlineMillis(context, job)))

    private fun sentKey(job: JobCardEntity) = "sent_${job.id}_${readyAt(job)}"
    private fun notifiedKey(job: JobCardEntity) = "notified_${job.id}_${readyAt(job)}"

    fun isSent(context: Context, job: JobCardEntity): Boolean =
        prefs(context).getBoolean(sentKey(job), false)

    fun markSent(context: Context, job: JobCardEntity) {
        prefs(context).edit().putBoolean(sentKey(job), true).apply()
    }

    fun lastNotified(context: Context, job: JobCardEntity): Long =
        prefs(context).getLong(notifiedKey(job), 0L)

    fun markNotified(context: Context, job: JobCardEntity) {
        prefs(context).edit().putLong(notifiedKey(job), System.currentTimeMillis()).apply()
    }
}
