package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditProfileState(
    val input: OwnerInput = OwnerInput(),
    val errors: OwnerInputErrors = OwnerInputErrors(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val hasOwner: Boolean = false,
    val loadFailed: Boolean = false,
    val repositoryError: RepositoryError? = null,
    val saved: Boolean = false,
    val openRegistration: Boolean = false,
)

class EditProfilePresenter(private val owners: OwnerRepository, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(EditProfileState())
    val state = mutableState.asStateFlow()
    private var owner: Owner? = null
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (loadJob?.isActive == true || state.value.saving) return
        mutableState.value = state.value.copy(loading = true, loadFailed = false, repositoryError = null)
        loadJob = scope.launch {
            when (val result = owners.observeOwner().first()) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(
                    loading = false, loadFailed = true, repositoryError = result.error,
                )
                is RepositoryResult.Success -> {
                    owner = result.value
                    mutableState.value = state.value.copy(
                        input = result.value?.toInput() ?: OwnerInput(),
                        loading = false,
                        hasOwner = result.value != null,
                    )
                }
            }
        }
    }

    fun changeInput(input: OwnerInput) {
        if (state.value.loading || state.value.saving || !state.value.hasOwner) return
        mutableState.value = state.value.copy(
            input = input,
            errors = state.value.errors.clearChanged(state.value.input, input),
            repositoryError = null,
        )
    }

    fun submit() {
        val current = state.value
        if (current.loading || current.saving) return
        if (current.loadFailed) {
            retry()
            return
        }
        val original = owner
        if (original == null) {
            mutableState.value = current.copy(openRegistration = true)
            return
        }
        val errors = OwnerFormValidator.validate(current.input)
        mutableState.value = current.copy(errors = errors, repositoryError = null)
        if (!errors.isEmpty) return
        val updatedOwner = OwnerFormValidator.owner(original.id, current.input)
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            when (val result = owners.save(updatedOwner)) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(
                    saving = false, repositoryError = result.error,
                )
                is RepositoryResult.Success -> {
                    owner = updatedOwner
                    mutableState.value = state.value.copy(saving = false, saved = true)
                }
            }
        }
    }

    fun takeSaved(): Boolean {
        if (!state.value.saved) return false
        mutableState.value = state.value.copy(saved = false)
        return true
    }

    fun takeRegistration(): Boolean {
        if (!state.value.openRegistration) return false
        mutableState.value = state.value.copy(openRegistration = false)
        return true
    }
}