package com.example.mobile.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mobile.MurPassportApplication
import kotlinx.coroutines.launch

class ReminderResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        val app = context.applicationContext as MurPassportApplication
        app.scope.launch {
            try {
                app.reminders.rebuild(reset = true)
            } finally {
                pending.finish()
            }
        }
    }
}