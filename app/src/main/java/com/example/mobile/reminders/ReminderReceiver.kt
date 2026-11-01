package com.example.mobile.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mobile.MurPassportApplication
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val identity = intent.getStringExtra(ReminderAlarms.EXTRA_IDENTITY) ?: return
        val at = intent.getLongExtra(ReminderAlarms.EXTRA_TRIGGER_AT, 0)
        val pending = goAsync()
        val app = context.applicationContext as MurPassportApplication
        app.scope.launch {
            try {
                app.reminders.deliver(identity, at)
            } finally {
                pending.finish()
            }
        }
    }
}