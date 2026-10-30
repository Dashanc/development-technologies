package com.example.mobile.presenter.health

import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PetHealthSummary(
    val pet: Pet,
    val recordCount: Int,
    val lastWeightKg: Double?,
    val nextEvent: UpcomingHealthEvent?,
)

data class HealthOverviewState(
    val pets: List<PetHealthSummary> = emptyList(),
    val events: List<UpcomingHealthEvent> = emptyList(),
    val loading: Boolean = true,
    val ownerMissing: Boolean = false,
    val error: RepositoryError? = null,
)

class HealthOverviewPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val records: MedicalRecordRepository,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val mutableState = MutableStateFlow(HealthOverviewState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var data: HealthData? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        data = null
        mutableState.value = HealthOverviewState()
        observation = scope.launch {
            observeHealthData(owners, pets, records).collect {
                data = it
                refresh()
            }
        }
    }

    fun refresh() {
        val data = data ?: return
        val events = data.items.upcomingEvents(LocalDate.now(clock))
        val itemsByPet = data.items.groupBy { it.record.petId }
        val eventsByPet = events.groupBy { it.petId }
        val summaries = if (data.error != null) emptyList() else data.pets.map { pet ->
            val items = itemsByPet[pet.id].orEmpty()
            PetHealthSummary(
                pet, items.size, items.firstNotNullOfOrNull { (it.record as? WeightRecord)?.weightKg },
                eventsByPet[pet.id]?.firstOrNull(),
            )
        }
        mutableState.value = HealthOverviewState(
            pets = summaries, events = events, loading = false, ownerMissing = data.ownerMissing, error = data.error,
        )
    }
}