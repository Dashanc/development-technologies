package com.example.mobile.presenter.pets

import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import com.example.mobile.presenter.health.HealthData
import com.example.mobile.presenter.health.observeHealthData
import com.example.mobile.presenter.health.upcomingEvents
import java.time.Clock
import java.time.LocalDate
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
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val mutableState = MutableStateFlow(PetListState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var data: HealthData? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        data = null
        mutableState.value = PetListState()
        observation = scope.launch {
            observeHealthData(owners, pets, records).combine(settings.observeSettings()) { data, preferences ->
                when (preferences) {
                    is RepositoryResult.Success -> data.copy(settings = preferences.value)
                    is RepositoryResult.Failure -> data.copy(items = emptyList(), error = data.error ?: preferences.error)
                }
            }.collect {
                data = it
                refresh()
            }
        }
    }

    fun refresh() {
        val data = data ?: return
        val today = LocalDate.now(clock)
        val itemsByPet = data.items.groupBy { it.record.petId }
        val eventsByPet = data.items.upcomingEvents(today).groupBy { it.petId }
        val cards = if (data.error != null) emptyList() else data.pets.map { pet ->
            val weight = itemsByPet[pet.id].orEmpty().firstNotNullOfOrNull { (it.record as? WeightRecord)?.weightKg }
            pet.toCard(today).copy(lastWeightKg = weight, nextEvent = eventsByPet[pet.id]?.firstOrNull())
        }
        mutableState.value = PetListState(
            pets = cards, loading = false, ownerMissing = data.ownerMissing, error = data.error,
            weightUnit = data.settings.weightUnit,
        )
    }
}
