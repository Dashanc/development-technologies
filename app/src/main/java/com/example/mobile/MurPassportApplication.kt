package com.example.mobile

import android.app.Application
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.reminders.ReminderCoordinator
import com.example.mobile.ui.common.AppAppearance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MurPassportApplication : Application() {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val repositories by lazy { LocalRepositories.get(this) }
    internal val reminders by lazy { ReminderCoordinator(this, repositories, scope) }
    private val appearance by lazy { AppAppearance(this, repositories.settings) }

    override fun onCreate() {
        super.onCreate()
        appearance.start(scope)
        reminders.start()
    }

    internal fun refreshSettings() {
        appearance.start(scope)
        reminders.refresh()
    }
}