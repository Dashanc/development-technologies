package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.model.AppSettings
import com.example.mobile.domain.model.AppTheme
import com.example.mobile.domain.model.ReminderLeadTime
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReminderStatus { READY, READ_FAILED, WRITE_FAILED, SCHEDULE_FAILED }

data class SettingsState(
    val settings: AppSettings? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: RepositoryError? = null,
    val permissionRequest: Boolean = false,
    val permissionBlocked: Boolean = false,
    val reminderStatus: ReminderStatus = ReminderStatus.READY,
)

class SettingsPresenter(
    private val repository: SettingsRepository,
    reminderStatus: Flow<ReminderStatus>,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(SettingsState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var notificationsAllowed = false

    init {
        retry()
        scope.launch { reminderStatus.collect { mutableState.value = state.value.copy(reminderStatus = it) } }
    }

    fun retry() {
        if (state.value.saving) return
        observation?.cancel()
        mutableState.value = state.value.copy(loading = true, error = null)
        observation = scope.launch {
            repository.observeSettings().collect { result ->
                mutableState.value = when (result) {
                    is RepositoryResult.Success -> state.value.copy(
                        settings = result.value, loading = false, error = null,
                        permissionBlocked = state.value.permissionBlocked || result.value.remindersEnabled && !notificationsAllowed,
                    )
                    is RepositoryResult.Failure -> state.value.copy(settings = null, loading = false, error = result.error)
                }
            }
        }
    }

    fun changeLanguage(value: AppLanguage) = update({ it.copy(language = value) }) { repository.setLanguage(value) }
    fun changeTheme(value: AppTheme) = update({ it.copy(theme = value) }) { repository.setTheme(value) }
    fun changeWeightUnit(value: WeightUnit) = update({ it.copy(weightUnit = value) }) { repository.setWeightUnit(value) }
    fun changeLeadTime(value: ReminderLeadTime) = update({ it.copy(reminderLeadTime = value) }) { repository.setReminderLeadTime(value) }

    fun changeReminders(enabled: Boolean, allowed: Boolean) {
        if (state.value.settings == null || state.value.saving) return
        if (enabled && !allowed) mutableState.value = state.value.copy(permissionRequest = true, permissionBlocked = false)
        else {
            mutableState.value = state.value.copy(permissionBlocked = false)
            update({ it.copy(remindersEnabled = enabled) }) { repository.setRemindersEnabled(enabled) }
        }
    }

    fun permissionResult(allowed: Boolean) {
        notificationsAllowed = allowed
        mutableState.value = state.value.copy(permissionRequest = false, permissionBlocked = !allowed)
        if (allowed) changeReminders(true, true)
    }

    fun refreshPermission(allowed: Boolean) {
        notificationsAllowed = allowed
        if (allowed || state.value.settings?.remindersEnabled == true) {
            mutableState.value = state.value.copy(permissionBlocked = !allowed)
        }
    }

    fun takePermissionRequest(): Boolean = state.value.permissionRequest.also {
        if (it) mutableState.value = state.value.copy(permissionRequest = false)
    }

    private fun update(change: (AppSettings) -> AppSettings, save: suspend () -> RepositoryResult<Unit>) {
        val current = state.value
        val settings = current.settings ?: return
        if (current.loading || current.saving || change(settings) == settings) return
        mutableState.value = current.copy(saving = true, error = null)
        scope.launch {
            mutableState.value = when (val result = save()) {
                is RepositoryResult.Success -> state.value.copy(settings = change(settings), saving = false, error = null)
                is RepositoryResult.Failure -> state.value.copy(saving = false, error = result.error)
            }
        }
    }
}