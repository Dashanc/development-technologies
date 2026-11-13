package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.AccountMode
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.repository.AuthenticationRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RegistrationState(
    val input: OwnerInput = OwnerInput(),
    val errors: OwnerInputErrors = OwnerInputErrors(),
    val serverErrors: Map<String, String> = emptyMap(),
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val login: Boolean = false,
    val consent: Boolean = false,
    val passwordInvalid: Boolean = false,
    val consentRequired: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loadFailed: Boolean = false,
    val repositoryError: RepositoryError? = null,
    val hasLocalProfile: Boolean = false,
    val hasOwner: Boolean = false,
    val openProfile: Boolean = false,
)

class RegistrationPresenter(
    private val authentication: AuthenticationRepository,
    private val localOwners: OwnerRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    private val resumeProfile: Boolean,
) {
    private val mutableState = MutableStateFlow(RegistrationState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init { retry() }

    fun retry() {
        if (loadJob?.isActive == true || state.value.saving) return
        mutableState.value = state.value.copy(loading = true, loadFailed = false, repositoryError = null)
        loadJob = scope.launch {
            val result = if (authentication.session.value.mode == AccountMode.RESTORING) {
                val restored = authentication.session.first { it.mode != AccountMode.RESTORING }
                restored.restoreError?.let { RepositoryResult.Failure(it) } ?: RepositoryResult.Success(Unit)
            } else authentication.restore()
            val preferences = settings.observeSettings().first()
            val local = localOwners.observeOwner().first()
            val active = authentication.session.value.mode in setOf(AccountMode.SERVER, AccountMode.LOCAL)
            val error = (result as? RepositoryResult.Failure)?.error ?: (preferences as? RepositoryResult.Failure)?.error
            mutableState.value = state.value.copy(
                loading = false, loadFailed = error != null, repositoryError = error,
                language = (preferences as? RepositoryResult.Success)?.value?.language ?: state.value.language,
                hasLocalProfile = (local as? RepositoryResult.Success)?.value != null,
                hasOwner = active, openProfile = resumeProfile && active,
            )
        }
    }

    fun changeInput(input: OwnerInput) {
        if (!canEdit()) return
        mutableState.value = state.value.copy(input = input, errors = state.value.errors.clearChanged(state.value.input, input),
            serverErrors = emptyMap(), repositoryError = null)
    }

    fun changeLanguage(language: AppLanguage) {
        if (canEdit()) mutableState.value = state.value.copy(language = language)
    }

    fun changeConsent(consent: Boolean) {
        if (canEdit()) mutableState.value = state.value.copy(consent = consent, consentRequired = false)
    }

    fun passwordChanged() {
        mutableState.value = state.value.copy(passwordInvalid = false, serverErrors = state.value.serverErrors - "password")
    }

    fun toggleLogin() {
        if (!canEdit()) return
        mutableState.value = state.value.copy(login = !state.value.login, errors = OwnerInputErrors(), serverErrors = emptyMap(),
            passwordInvalid = false, consentRequired = false, repositoryError = null)
    }

    fun submit(password: CharArray): Boolean {
        val current = state.value
        if (current.hasOwner) {
            password.fill('\u0000')
            mutableState.value = current.copy(openProfile = true)
            return false
        }
        if (!canEdit()) {
            password.fill('\u0000')
            if (current.loadFailed) retry()
            return false
        }
        val allErrors = OwnerFormValidator.validate(current.input)
        val errors = if (current.login) OwnerInputErrors(email = allErrors.email) else allErrors
        val length = password.concatToString().let { it.codePointCount(0, it.length) }
        val invalid = if (current.login) length !in 1..128 else length !in 8..128
        val consentRequired = !current.login && !current.consent
        mutableState.value = current.copy(errors = errors, passwordInvalid = invalid, consentRequired = consentRequired,
            repositoryError = null, serverErrors = emptyMap())
        if (!errors.isEmpty || invalid || consentRequired) {
            password.fill('\u0000')
            return false
        }
        mutableState.value = state.value.copy(saving = true)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val preferenceResult = settings.setLanguage(current.language)
                if (preferenceResult is RepositoryResult.Failure) {
                    fail(preferenceResult)
                    return@launch
                }
                val result = if (current.login) authentication.login(current.input.email.trim(), password) else {
                    val owner = OwnerFormValidator.owner("", current.input)
                    authentication.register(owner.name, owner.email, owner.phone, password)
                }
                when (result) {
                    is RepositoryResult.Failure -> fail(result)
                    is RepositoryResult.Success -> mutableState.value = state.value.copy(saving = false, hasOwner = true, openProfile = true)
                }
            } finally { password.fill('\u0000') }
        }
        return true
    }

    fun openLocalProfile() {
        if (state.value.loading || state.value.saving || !state.value.hasLocalProfile) return
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            when (val result = authentication.openLocalProfile()) {
                is RepositoryResult.Failure -> fail(result)
                is RepositoryResult.Success -> mutableState.value = state.value.copy(saving = false, openProfile = true)
            }
        }
    }

    fun forgetSession() {
        if (state.value.loading || state.value.saving) return
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            when (val result = authentication.logout()) {
                is RepositoryResult.Failure -> fail(result)
                is RepositoryResult.Success -> { mutableState.value = state.value.copy(saving = false); retry() }
            }
        }
    }

    fun takeNavigation(): Boolean {
        if (!state.value.openProfile) return false
        mutableState.value = state.value.copy(openProfile = false)
        return true
    }

    private fun canEdit() = !state.value.loading && !state.value.saving && !state.value.loadFailed && !state.value.hasOwner
    private fun fail(failure: RepositoryResult.Failure) {
        mutableState.value = state.value.copy(saving = false, repositoryError = failure.error, serverErrors = failure.fieldErrors)
    }
}