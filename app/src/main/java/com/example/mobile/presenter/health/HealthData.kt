package com.example.mobile.presenter.health

import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.AppSettings
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class MedicalHistoryItem(val record: MedicalRecord, val petName: String)

internal data class HealthData(
    val pets: List<Pet> = emptyList(),
    val items: List<MedicalHistoryItem> = emptyList(),
    val ownerMissing: Boolean = false,
    val noPets: Boolean = false,
    val petMissing: Boolean = false,
    val error: RepositoryError? = null,
    val settings: AppSettings = AppSettings(),
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeHealthData(
    owners: OwnerRepository,
    pets: PetRepository,
    records: MedicalRecordRepository,
    petId: String? = null,
): Flow<HealthData> = owners.observeOwner().flatMapLatest { ownerResult ->
    when (ownerResult) {
        is RepositoryResult.Failure -> flowOf(HealthData(error = ownerResult.error))
        is RepositoryResult.Success -> {
            val owner = ownerResult.value
            if (owner == null) flowOf(HealthData(ownerMissing = true))
            else pets.observePets(owner.id).flatMapLatest { petResult ->
                when (petResult) {
                    is RepositoryResult.Failure -> flowOf(HealthData(error = petResult.error))
                    is RepositoryResult.Success -> {
                        val selected = petResult.value.filter { it.ownerId == owner.id && (petId == null || it.id == petId) }
                        if (selected.isEmpty()) flowOf(HealthData(noPets = petResult.value.isEmpty(), petMissing = petId != null))
                        else combine(selected.map { records.observeRecords(it.id) }) { results ->
                            val error = results.filterIsInstance<RepositoryResult.Failure>().firstOrNull()?.error
                            val items = if (error != null) emptyList() else selected.zip(results.toList()).flatMap { (pet, result) ->
                                if (result is RepositoryResult.Success) result.value.filter { it.petId == pet.id }
                                    .map { MedicalHistoryItem(it, pet.name) } else emptyList()
                            }.asReversed().sortedByDescending { it.record.date }
                            HealthData(pets = selected, items = items, error = error)
                        }
                    }
                }
            }
        }
    }
}