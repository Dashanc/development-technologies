package com.example.mobile.presenter.health

import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MedicalHistoryItem(val record: MedicalRecord, val petName: String)

data class HealthHistoryState(
    val items: List<MedicalHistoryItem> = emptyList(),
    val petName: String? = null,
    val loading: Boolean = true,
    val ownerMissing: Boolean = false,
    val noPets: Boolean = false,
    val petMissing: Boolean = false,
    val error: RepositoryError? = null,
)

class HealthHistoryPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val records: MedicalRecordRepository,
    private val petId: String?,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(HealthHistoryState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        mutableState.value = HealthHistoryState()
        observation = scope.launch {
            owners.observeOwner().collectLatest { ownerResult ->
                when (ownerResult) {
                    is RepositoryResult.Failure -> fail(ownerResult.error)
                    is RepositoryResult.Success -> {
                        val owner = ownerResult.value
                        if (owner == null) mutableState.value = HealthHistoryState(loading = false, ownerMissing = true)
                        else pets.observePets(owner.id).collectLatest { petResult ->
                            when (petResult) {
                                is RepositoryResult.Failure -> fail(petResult.error)
                                is RepositoryResult.Success -> {
                                    val selected = petResult.value.filter { petId == null || it.id == petId }
                                    if (selected.isEmpty()) {
                                        mutableState.value = HealthHistoryState(
                                            loading = false, noPets = petResult.value.isEmpty(), petMissing = petId != null,
                                        )
                                    } else combine(selected.map { records.observeRecords(it.id) }) { results ->
                                        val error = results.filterIsInstance<RepositoryResult.Failure>().firstOrNull()?.error
                                        val items = selected.zip(results.toList()).flatMap { (pet, result) ->
                                            if (result is RepositoryResult.Success) result.value.map { MedicalHistoryItem(it, pet.name) } else emptyList()
                                        }.asReversed().sortedByDescending { it.record.date }
                                        HealthHistoryState(
                                            if (error == null) items else emptyList(),
                                            selected.singleOrNull()?.name.takeIf { petId != null }, loading = false, error = error,
                                        )
                                    }.collect { mutableState.value = it }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fail(error: RepositoryError) {
        mutableState.value = HealthHistoryState(loading = false, error = error)
    }
}