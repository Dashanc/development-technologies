package com.example.mobile.presenter.pets

import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetPhotoRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class EditPetDestination { SAVED, REGISTRATION, PET_LIST }

data class EditPetState(
    val input: PetInput = PetInput(),
    val errors: PetInputErrors = PetInputErrors(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val hasPet: Boolean = false,
    val loadFailed: Boolean = false,
    val ownerMissing: Boolean = false,
    val petMissing: Boolean = false,
    val photoLoading: Boolean = false,
    val photoFailed: Boolean = false,
    val error: RepositoryError? = null,
    val destination: EditPetDestination? = null,
) {
    val editable get() = hasPet && !loading && !saving && !saved && !loadFailed && !ownerMissing && !petMissing && destination == null
}

class EditPetPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val photos: PetPhotoRepository,
    private val petId: String?,
    private val scope: CoroutineScope,
    private val restoredInput: PetInput? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val mutableState = MutableStateFlow(EditPetState(input = restoredInput ?: PetInput()))
    val state = mutableState.asStateFlow()
    private var original: Pet? = null
    private var loadJob: Job? = null
    private var pendingPhoto: String? = null

    init {
        retry()
    }

    fun retry() {
        if (loadJob?.isActive == true || state.value.saving || state.value.saved || state.value.photoLoading || state.value.destination != null) return
        mutableState.value = state.value.copy(loading = true, loadFailed = false, error = null, ownerMissing = false, petMissing = false)
        if (petId.isNullOrBlank()) {
            mutableState.value = state.value.copy(loading = false, hasPet = false, petMissing = true)
            return
        }
        loadJob = scope.launch {
            when (val ownerResult = owners.observeOwner().first()) {
                is RepositoryResult.Failure -> loadFailed(ownerResult.error)
                is RepositoryResult.Success -> {
                    val owner = ownerResult.value
                    if (owner == null) {
                        mutableState.value = state.value.copy(loading = false, hasPet = false, ownerMissing = true)
                        return@launch
                    }
                    when (val petResult = pets.observePet(petId).first()) {
                        is RepositoryResult.Failure -> loadFailed(petResult.error)
                        is RepositoryResult.Success -> {
                            val pet = petResult.value?.takeIf {
                                it.id == petId && it.ownerId == owner.id && (original == null || it.ownerId == original?.ownerId)
                            }
                            if (pet == null) {
                                mutableState.value = state.value.copy(loading = false, hasPet = false, petMissing = true)
                            } else {
                                val input = if (original == null) restoredInput ?: pet.toInput() else state.value.input
                                original = pet
                                mutableState.value = state.value.copy(input = input, loading = false, hasPet = true)
                                pendingPhoto?.let {
                                    pendingPhoto = null
                                    selectPhoto(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadFailed(error: RepositoryError) {
        mutableState.value = state.value.copy(loading = false, loadFailed = true, error = error)
    }

    fun changeInput(input: PetInput) {
        if (!state.value.editable) return
        val previous = state.value.input
        mutableState.value = state.value.copy(
            input = input.copy(photoUri = previous.photoUri),
            errors = state.value.errors.clearChanged(previous, input),
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
        val today = LocalDate.now(clock)
        return PetFormValidator.birthDate(state.value.input.birth, today)?.takeUnless { it.isAfter(today) } ?: today
    }

    fun selectPhoto(uri: String) {
        if (state.value.loading) {
            pendingPhoto = uri
            return
        }
        if (!state.value.editable || state.value.photoLoading) return
        mutableState.value = state.value.copy(photoLoading = true, photoFailed = false)
        scope.launch {
            when (val result = photos.retain(uri)) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(photoLoading = false, photoFailed = true)
                is RepositoryResult.Success -> mutableState.value = state.value.copy(
                    input = state.value.input.copy(photoUri = result.value), photoLoading = false, photoFailed = false, error = null,
                )
            }
        }
    }

    fun removePhoto() {
        if (!state.value.editable || state.value.photoLoading) return
        mutableState.value = state.value.copy(input = state.value.input.copy(photoUri = null), photoFailed = false, error = null)
    }

    fun submit() {
        val current = state.value
        if (current.loading || current.saving || current.saved || current.photoLoading || current.destination != null) return
        when {
            current.loadFailed -> { retry(); return }
            current.ownerMissing -> {
                mutableState.value = current.copy(destination = EditPetDestination.REGISTRATION)
                return
            }
            current.petMissing || !current.hasPet -> {
                mutableState.value = current.copy(destination = EditPetDestination.PET_LIST)
                return
            }
        }
        val source = original ?: return
        val today = LocalDate.now(clock)
        val errors = PetFormValidator.validate(current.input, today)
        mutableState.value = current.copy(errors = errors, error = null)
        if (!errors.isEmpty) return
        val updated = PetFormValidator.pet(source.id, source.ownerId, current.input, today)
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            if (updated.photoUri != null && updated.photoUri != source.photoUri && photos.retain(updated.photoUri) is RepositoryResult.Failure) {
                mutableState.value = state.value.copy(saving = false, photoFailed = true)
                return@launch
            }
            when (val result = pets.update(updated)) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(
                    saving = false,
                    error = result.error.takeUnless { it == RepositoryError.INVALID_REFERENCE },
                    petMissing = result.error == RepositoryError.INVALID_REFERENCE,
                )
                is RepositoryResult.Success -> {
                    original = updated
                    mutableState.value = state.value.copy(saving = false, saved = true, destination = EditPetDestination.SAVED)
                }
            }
        }
    }

    fun currentDraft(): PetInput? = state.value.input.takeIf {
        !state.value.saved && (original != null || restoredInput != null)
    }

    fun takeDestination(): EditPetDestination? = state.value.destination?.also {
        mutableState.value = state.value.copy(destination = null)
    }
}