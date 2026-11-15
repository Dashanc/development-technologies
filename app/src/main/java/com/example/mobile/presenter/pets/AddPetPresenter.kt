package com.example.mobile.presenter.pets

import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetPhotoRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class AddPetState(
    val input: PetInput = PetInput(),
    val errors: PetInputErrors = PetInputErrors(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val photoLoading: Boolean = false,
    val photoFailed: Boolean = false,
    val ownerMissing: Boolean = false,
    val loadFailed: Boolean = false,
    val error: RepositoryError? = null,
    val savedPetId: String? = null,
    val openRegistration: Boolean = false,
) {
    val editable get() = !loading && !saving && !loadFailed && !ownerMissing
}

class AddPetPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val photos: PetPhotoRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(AddPetState())
    val state = mutableState.asStateFlow()
    private val newPetId = UUID.randomUUID().toString()
    private var ownerId: String? = null
    private var loading: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (loading?.isActive == true || state.value.saving) return
        mutableState.value = state.value.copy(loading = true, loadFailed = false, error = null)
        loading = scope.launch {
            when (val result = owners.observeOwner().first()) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(
                    loading = false, loadFailed = true, error = result.error,
                )
                is RepositoryResult.Success -> {
                    ownerId = result.value?.id
                    mutableState.value = state.value.copy(loading = false, ownerMissing = result.value == null)
                }
            }
        }
    }

    fun changeInput(input: PetInput) {
        if (!state.value.editable) return
        val previous = state.value.input
        val errors = state.value.errors
        mutableState.value = state.value.copy(
            input = input.copy(photoUri = previous.photoUri),
            errors = errors.copy(
                name = errors.name.takeIf { previous.name == input.name },
                birth = errors.birth.takeIf { previous.birth == input.birth },
                sex = errors.sex.takeIf { previous.sex == input.sex },
                microchip = errors.microchip.takeIf { previous.extra.microchip == input.extra.microchip },
            ),
            error = null,
        )
    }

    fun changeExtra(extra: PetExtraData) = changeInput(state.value.input.copy(extra = extra))

    fun validateExtra(): Boolean {
        val error = PetFormValidator.microchip(state.value.input.extra.microchip)
        mutableState.value = state.value.copy(errors = state.value.errors.copy(microchip = error))
        return error == null
    }

    fun calendarDate(): LocalDate {
        val today = LocalDate.now()
        return PetFormValidator.birthDate(state.value.input.birth, today)?.takeUnless { it.isAfter(today) } ?: today
    }

    fun selectPhoto(uri: String) {
        if (state.value.saving || state.value.photoLoading || state.value.ownerMissing) return
        mutableState.value = state.value.copy(photoLoading = true, photoFailed = false)
        scope.launch {
            when (val result = photos.retain(uri)) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(photoLoading = false, photoFailed = true)
                is RepositoryResult.Success -> mutableState.value = state.value.copy(
                    input = state.value.input.copy(photoUri = result.value), photoLoading = false, photoFailed = false,
                )
            }
        }
    }

    fun removePhoto() {
        if (state.value.editable && !state.value.photoLoading) {
            mutableState.value = state.value.copy(input = state.value.input.copy(photoUri = null), photoFailed = false)
        }
    }

    fun submit() {
        val current = state.value
        if (current.loading || current.saving || current.photoLoading) return
        if (current.loadFailed) {
            retry()
            return
        }
        val owner = ownerId
        if (owner == null) {
            mutableState.value = current.copy(openRegistration = true)
            return
        }
        val today = LocalDate.now()
        val errors = PetFormValidator.validate(current.input, today)
        mutableState.value = current.copy(errors = errors, error = null)
        if (!errors.isEmpty) return
        val pet = PetFormValidator.pet(newPetId, owner, current.input, today)
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            when (val result = pets.save(pet)) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(saving = false, error = result.error)
                is RepositoryResult.Success -> mutableState.value = state.value.copy(saving = false, savedPetId = pet.id)
            }
        }
    }

    fun takeSavedPet(): String? = state.value.savedPetId?.also {
        mutableState.value = state.value.copy(savedPetId = null)
    }

    fun takeRegistration(): Boolean {
        if (!state.value.openRegistration) return false
        mutableState.value = state.value.copy(openRegistration = false)
        return true
    }
}