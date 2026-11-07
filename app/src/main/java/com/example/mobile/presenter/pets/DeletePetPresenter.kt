package com.example.mobile.presenter.pets

import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetPhotoRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.ReminderScheduleRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DeletePetState(
    val pet: Pet? = null,
    val loading: Boolean = true,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val missing: Boolean = false,
    val finished: Boolean = false,
    val cleanupFailed: Boolean = false,
    val error: RepositoryError? = null,
)

class DeletePetPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val photos: PetPhotoRepository,
    private val reminders: ReminderScheduleRepository,
    private val petId: String?,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(DeletePetState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (loadJob?.isActive == true || state.value.deleting || state.value.deleted || state.value.finished) return
        mutableState.value = DeletePetState()
        if (petId.isNullOrBlank()) {
            mutableState.value = DeletePetState(loading = false, missing = true)
            return
        }
        loadJob = scope.launch {
            when (val ownerResult = owners.observeOwner().first()) {
                is RepositoryResult.Failure -> mutableState.value = DeletePetState(loading = false, error = ownerResult.error)
                is RepositoryResult.Success -> {
                    val owner = ownerResult.value
                    if (owner == null) {
                        mutableState.value = DeletePetState(loading = false, missing = true)
                        return@launch
                    }
                    when (val result = pets.observePet(petId).first()) {
                        is RepositoryResult.Failure -> mutableState.value = DeletePetState(loading = false, error = result.error)
                        is RepositoryResult.Success -> {
                            val pet = result.value?.takeIf { it.id == petId && it.ownerId == owner.id }
                            mutableState.value = DeletePetState(pet = pet, loading = false, missing = pet == null)
                        }
                    }
                }
            }
        }
    }

    fun confirmDelete() {
        val current = state.value
        if (current.loading || current.deleting || current.finished || current.missing) return
        val pet = current.pet ?: return
        mutableState.value = current.copy(deleting = true, error = null, cleanupFailed = false)
        scope.launch {
            if (!current.deleted) {
                when (val result = pets.delete(pet.id)) {
                    is RepositoryResult.Failure -> {
                        mutableState.value = state.value.copy(deleting = false, error = result.error)
                        return@launch
                    }
                    is RepositoryResult.Success -> mutableState.value = state.value.copy(deleted = true)
                }
            }
            val reminderResult = reminders.refreshSchedule()
            val photoResult = pet.photoUri?.let { photos.releaseIfUnused(it) } ?: RepositoryResult.Success(Unit)
            val failed = reminderResult is RepositoryResult.Failure || photoResult is RepositoryResult.Failure
            mutableState.value = state.value.copy(deleting = false, cleanupFailed = failed, finished = !failed)
        }
    }
}