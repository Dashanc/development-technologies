package com.example.mobile.presenter.health

import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.SettingsRepository
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class MedicalHistoryFilter { ALL, WEIGHT, VACCINATION, VET_VISIT }

data class HealthHistoryState(
    val items: List<MedicalHistoryItem> = emptyList(),
    val petName: String? = null,
    val filter: MedicalHistoryFilter = MedicalHistoryFilter.ALL,
    val loading: Boolean = true,
    val ownerMissing: Boolean = false,
    val noPets: Boolean = false,
    val petMissing: Boolean = false,
    val error: RepositoryError? = null,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
)

class HealthHistoryPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val records: MedicalRecordRepository,
    private val petId: String?,
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
) {
    private val mutableState = MutableStateFlow(HealthHistoryState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var data: HealthData? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        data = null
        mutableState.value = HealthHistoryState(filter = state.value.filter)
        observation = scope.launch {
            observeHealthData(owners, pets, records, petId).combine(settings.observeSettings()) { data, result ->
                when (result) {
                    is RepositoryResult.Success -> data.copy(settings = result.value)
                    is RepositoryResult.Failure -> data.copy(items = emptyList(), error = data.error ?: result.error)
                }
            }.collect {
                data = it
                render()
            }
        }
    }

    fun selectFilter(filter: MedicalHistoryFilter) {
        if (state.value.filter == filter) return
        mutableState.value = state.value.copy(filter = filter)
        render()
    }

    private fun render() {
        val data = data ?: return
        val filter = state.value.filter
        val items = data.items.filter {
            when (filter) {
                MedicalHistoryFilter.ALL -> true
                MedicalHistoryFilter.WEIGHT -> it.record is WeightRecord
                MedicalHistoryFilter.VACCINATION -> it.record is VaccinationRecord
                MedicalHistoryFilter.VET_VISIT -> it.record is VetVisitRecord
            }
        }
        mutableState.value = HealthHistoryState(
            items = items, petName = data.pets.singleOrNull()?.name.takeIf { petId != null },
            filter = filter, loading = false, ownerMissing = data.ownerMissing,
            noPets = data.noPets, petMissing = data.petMissing, error = data.error,
            weightUnit = data.settings.weightUnit,
        )
    }
}