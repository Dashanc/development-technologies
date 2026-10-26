package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class RegistrationMessage { EXISTING_PROFILE, NO_PROFILE }

data class RegistrationState(
    val input: OwnerInput = OwnerInput(),
    val errors: OwnerInputErrors = OwnerInputErrors(),
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val consent: Boolean = false,
    val passwordTooShort: Boolean = false,
    val consentRequired: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loadFailed: Boolean = false,
    val repositoryError: RepositoryError? = null,
    val message: RegistrationMessage? = null,
    val hasOwner: Boolean = false,
    val openProfile: Boolean = false,
)

class RegistrationPresenter(
    private val owners: OwnerRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    private val resumeProfile: Boolean,
) {
    private val mutableState = MutableStateFlow(RegistrationState())
    val state = mutableState.asStateFlow()
    private var owner: Owner? = null
    private val newOwnerId = UUID.randomUUID().toString()
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (loadJob?.isActive == true || state.value.saving) return
        mutableState.value = state.value.copy(loading = true, loadFailed = false, repositoryError = null)
        loadJob = scope.launch {
            when (val result = owners.observeOwner().first()) {
                is RepositoryResult.Failure -> {
                    failLoading(result.error)
                    return@launch
                }
                is RepositoryResult.Success -> owner = result.value
            }
            when (val result = settings.observeSettings().first()) {
                is RepositoryResult.Failure -> failLoading(result.error)
                is RepositoryResult.Success -> mutableState.value = state.value.copy(
                    loading = false,
                    language = result.value.language,
                    hasOwner = owner != null,
                    message = if (owner != null) RegistrationMessage.EXISTING_PROFILE else null,
                    openProfile = resumeProfile && owner != null,
                )
            }
        }
    }

    fun changeInput(input: OwnerInput) {
        if (!canEdit()) return
        mutableState.value = state.value.copy(
            input = input,
            errors = state.value.errors.clearChanged(state.value.input, input),
            repositoryError = null,
            message = null,
        )
    }

    fun changeLanguage(language: AppLanguage) {
        if (canEdit()) mutableState.value = state.value.copy(language = language)
    }

    fun changeConsent(consent: Boolean) {
        if (canEdit()) mutableState.value = state.value.copy(consent = consent, consentRequired = false)
    }

    fun passwordChanged() {
        if (state.value.passwordTooShort) mutableState.value = state.value.copy(passwordTooShort = false)
    }

    fun submit(password: CharSequence) {
        if (state.value.loading || state.value.saving) return
        if (state.value.loadFailed) {
            retry()
            return
        }
        if (owner != null) {
            openExistingProfile()
            return
        }
        val current = state.value
        val errors = OwnerFormValidator.validate(current.input)
        val passwordTooShort = password.length < 8 || password.isBlank()
        mutableState.value = current.copy(
            errors = errors,
            passwordTooShort = passwordTooShort,
            consentRequired = !current.consent,
            repositoryError = null,
            message = null,
        )
        if (!errors.isEmpty || passwordTooShort || !current.consent) return
        val newOwner = OwnerFormValidator.owner(newOwnerId, current.input)
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            when (val result = settings.setLanguage(current.language)) {
                is RepositoryResult.Failure -> {
                    failSaving(result.error)
                    return@launch
                }
                is RepositoryResult.Success -> Unit
            }
            when (val result = owners.save(newOwner)) {
                is RepositoryResult.Failure -> failSaving(result.error)
                is RepositoryResult.Success -> {
                    owner = newOwner
                    mutableState.value = state.value.copy(saving = false, hasOwner = true, openProfile = true)
                }
            }
        }
    }

    fun openExistingProfile() {
        if (state.value.loading || state.value.saving) return
        if (state.value.loadFailed) {
            retry()
        } else if (owner != null) {
            mutableState.value = state.value.copy(openProfile = true)
        } else {
            mutableState.value = state.value.copy(message = RegistrationMessage.NO_PROFILE)
        }
    }

    fun takeNavigation(): Boolean {
        if (!state.value.openProfile) return false
        mutableState.value = state.value.copy(openProfile = false)
        return true
    }

    private fun canEdit() = !state.value.loading && !state.value.saving && !state.value.loadFailed && owner == null

    private fun failLoading(error: RepositoryError) {
        mutableState.value = state.value.copy(loading = false, loadFailed = true, repositoryError = error)
    }

    private fun failSaving(error: RepositoryError) {
        mutableState.value = state.value.copy(saving = false, repositoryError = error)
        if (error == RepositoryError.IDENTITY_CONFLICT) retry()
    }
}