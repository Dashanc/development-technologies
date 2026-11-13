package com.example.mobile

import android.app.Application
import com.example.mobile.data.ApplicationRepositories
import com.example.mobile.domain.model.AccountMode
import com.example.mobile.reminders.ReminderCoordinator
import com.example.mobile.ui.common.AppAppearance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MurPassportApplication : Application() {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val repositories: ApplicationRepositories by lazy { ApplicationRepositories(this, scope) { reminders.clearAccount() } }
    internal val reminders: ReminderCoordinator by lazy {
        ReminderCoordinator(this, repositories.owner, repositories.pets, repositories.medicalRecords,
            repositories.settings, repositories.reminderStorage, scope)
    }
    private val appearance by lazy { AppAppearance(this, repositories.settings) }

    override fun onCreate() {
        super.onCreate()
        appearance.start(scope)
        scope.launch { repositories.authentication.restore() }
        scope.launch {
            repositories.authentication.session.map { it.mode }.distinctUntilChanged().collectLatest {
                if (it != AccountMode.RESTORING) reminders.start()
            }
        }
    }

    internal fun refreshSettings() {
        appearance.start(scope)
        reminders.refresh()
    }
}