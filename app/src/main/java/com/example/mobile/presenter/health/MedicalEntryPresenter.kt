package com.example.mobile.presenter.health

import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.AppSettings
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class MedicalDestination { REGISTRATION, ADD_PET }

data class MedicalEntryState<I>(
    val input: I,
    val pets: List<Pet> = emptyList(),
    val selectedPetId: String? = null,
    val errors: Map<MedicalField, MedicalInputError> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loadFailed: Boolean = false,
    val ownerMissing: Boolean = false,
    val error: RepositoryError? = null,
    val savedPetId: String? = null,
    val destination: MedicalDestination? = null,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
) {
    val editable get() = !loading && !saving && !loadFailed && !ownerMissing && pets.isNotEmpty()
}

abstract class MedicalEntryPresenter<I>(
    initial: I,
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val records: MedicalRecordRepository,
    private val initialPetId: String?,
    private val scope: CoroutineScope,
    private val settings: SettingsRepository? = null,
) {
    private val mutableState = MutableStateFlow(MedicalEntryState(initial))
    val state = mutableState.asStateFlow()
    private val recordId = UUID.randomUUID().toString()
    private var observation: Job? = null
    private var selectionInitialized = false

    init {
        retry()
    }

    fun retry() {
        if (state.value.saving) return
        observation?.cancel()
        mutableState.value = state.value.copy(loading = true, loadFailed = false, error = null)
        observation = scope.launch {
            val preferences = settings?.observeSettings() ?: flowOf(RepositoryResult.Success(AppSettings()))
            owners.observeOwner().combine(preferences) { owner, settings -> owner to settings }.collectLatest { (ownerResult, settingsResult) ->
                if (settingsResult is RepositoryResult.Failure) {
                    failLoading(settingsResult.error)
                    return@collectLatest
                }
                val unit = (settingsResult as RepositoryResult.Success).value.weightUnit
                mutableState.value = state.value.copy(input = unitChanged(state.value.input, state.value.weightUnit, unit), weightUnit = unit)
                when (ownerResult) {
                    is RepositoryResult.Failure -> failLoading(ownerResult.error)
                    is RepositoryResult.Success -> {
                        val owner = ownerResult.value
                        if (owner == null) mutableState.value = state.value.copy(
                            loading = false, ownerMissing = true, pets = emptyList(), selectedPetId = null,
                        ) else pets.observePets(owner.id).collect { result ->
                            when (result) {
                                is RepositoryResult.Failure -> failLoading(result.error)
                                is RepositoryResult.Success -> {
                                    val selected = if (selectionInitialized) state.value.selectedPetId else initialPetId
                                    selectionInitialized = true
                                    mutableState.value = state.value.copy(
                                        pets = result.value,
                                        selectedPetId = selected?.takeIf { id -> result.value.any { it.id == id } },
                                        loading = false, loadFailed = false, ownerMissing = false, error = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun changeInput(input: I, field: MedicalField? = null) {
        if (!state.value.editable || state.value.input == input) return
        mutableState.value = state.value.copy(
            input = input, errors = if (field == null) state.value.errors else state.value.errors - field, error = null,
        )
    }

    fun selectPet(id: String?) {
        if (!state.value.editable || id != null && state.value.pets.none { it.id == id }) return
        mutableState.value = state.value.copy(selectedPetId = id, errors = state.value.errors - MedicalField.PET)
    }

    fun calendarDate(field: MedicalField): LocalDate = MedicalValidation.parseDate(dateText(state.value.input, field)) ?: LocalDate.now()

    fun submit() {
        val current = state.value
        if (current.loading || current.saving || current.savedPetId != null) return
        if (current.loadFailed) {
            retry()
            return
        }
        if (current.ownerMissing || current.pets.isEmpty()) {
            mutableState.value = current.copy(destination = if (current.ownerMissing) MedicalDestination.REGISTRATION else MedicalDestination.ADD_PET)
            return
        }
        val petId = current.selectedPetId
        if (petId == null || current.pets.none { it.id == petId }) {
            mutableState.value = current.copy(errors = mapOf(MedicalField.PET to MedicalInputError.PET_REQUIRED))
            return
        }
        val prepared = prepare(recordId, petId, current.input)
        mutableState.value = current.copy(errors = prepared.errors, error = null)
        val record = prepared.record ?: return
        mutableState.value = state.value.copy(saving = true)
        scope.launch {
            when (val result = records.save(record)) {
                is RepositoryResult.Failure -> mutableState.value = state.value.copy(
                    saving = false, error = result.error, loadFailed = result.error == RepositoryError.INVALID_REFERENCE,
                )
                is RepositoryResult.Success -> mutableState.value = state.value.copy(saving = false, savedPetId = record.petId)
            }
        }
    }

    fun takeSavedPet(): String? = state.value.savedPetId?.also { mutableState.value = state.value.copy(savedPetId = null) }

    fun takeDestination(): MedicalDestination? = state.value.destination?.also {
        mutableState.value = state.value.copy(destination = null)
    }

    protected abstract fun prepare(id: String, petId: String, input: I): PreparedMedicalRecord
    protected abstract fun dateText(input: I, field: MedicalField): String
    protected open fun unitChanged(input: I, from: WeightUnit, to: WeightUnit): I = input

    private fun failLoading(error: RepositoryError) {
        mutableState.value = state.value.copy(loading = false, loadFailed = true, error = error)
    }
}