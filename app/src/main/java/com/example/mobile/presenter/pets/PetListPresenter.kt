package com.example.mobile.presenter.pets

import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import com.example.mobile.presenter.health.observeHealthData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PetListState(
    val pets: List<PetCard> = emptyList(),
    val loading: Boolean = true,
    val ownerMissing: Boolean = false,
    val error: RepositoryError? = null,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
)

class PetListPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val records: MedicalRecordRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(PetListState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        mutableState.value = PetListState()
        observation = scope.launch {
            observeHealthData(owners, pets, records).combine(settings.observeSettings()) { data, preferences ->
                val error = data.error ?: (preferences as? RepositoryResult.Failure)?.error
                val unit = (preferences as? RepositoryResult.Success)?.value?.weightUnit ?: WeightUnit.KILOGRAM
                val itemsByPet = data.items.groupBy { it.record.petId }
                val cards = if (error != null) emptyList() else data.pets.map { pet ->
                    val weight = itemsByPet[pet.id].orEmpty().firstNotNullOfOrNull { (it.record as? WeightRecord)?.weightKg }
                    pet.toCard().copy(lastWeightKg = weight)
                }
                PetListState(pets = cards, loading = false, ownerMissing = data.ownerMissing, error = error, weightUnit = unit)
            }.collect { mutableState.value = it }
        }
    }
}