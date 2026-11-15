package com.example.mobile.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

internal class ReminderAlarms(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun schedule(plan: ReminderPlan) {
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAt.toEpochMilli(), requireNotNull(pending(plan.identity, plan.triggerAt.toEpochMilli())))
    }

    fun exists(identity: String): Boolean = pending(identity, flags = PendingIntent.FLAG_NO_CREATE) != null

    fun cancel(identity: String) {
        pending(identity, flags = PendingIntent.FLAG_NO_CREATE)?.let {
            manager.cancel(it)
            it.cancel()
        }
    }

    private fun pending(identity: String, at: Long = 0, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.Builder().scheme("murpassport").authority("reminder").appendPath(identity).build()
            putExtra(EXTRA_IDENTITY, identity)
            putExtra(EXTRA_TRIGGER_AT, at)
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val EXTRA_IDENTITY = "reminder_identity"
        const val EXTRA_TRIGGER_AT = "reminder_trigger_at"
    }
}