package com.example.mobile.presenter.pets

import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PetProfileState(
    val card: PetCard? = null,
    val loading: Boolean = true,
    val ownerMissing: Boolean = false,
    val error: RepositoryError? = null,
    val lastWeightKg: Double? = null,
    val recordCount: Int? = null,
)

class PetProfilePresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val records: MedicalRecordRepository,
    private val petId: String?,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(PetProfileState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        mutableState.value = PetProfileState()
        if (petId.isNullOrBlank()) {
            mutableState.value = PetProfileState(loading = false)
            return
        }
        observation = scope.launch {
            owners.observeOwner().collectLatest { result ->
                when (result) {
                    is RepositoryResult.Failure -> mutableState.value = PetProfileState(loading = false, error = result.error)
                    is RepositoryResult.Success -> {
                        val owner = result.value
                        if (owner == null) mutableState.value = PetProfileState(loading = false, ownerMissing = true)
                        else pets.observePet(petId).collectLatest { petResult ->
                            when (petResult) {
                                is RepositoryResult.Failure -> mutableState.value = PetProfileState(loading = false, error = petResult.error)
                                is RepositoryResult.Success -> {
                                    val card = petResult.value?.takeIf { it.ownerId == owner.id }?.toCard()
                                    mutableState.value = PetProfileState(card = card, loading = false)
                                    if (card != null) records.observeRecords(card.pet.id).collect { recordResult ->
                                        mutableState.value = when (recordResult) {
                                            is RepositoryResult.Failure -> state.value.copy(error = recordResult.error)
                                            is RepositoryResult.Success -> state.value.copy(
                                                lastWeightKg = recordResult.value.asReversed().filterIsInstance<WeightRecord>().maxByOrNull { it.date }?.weightKg,
                                                recordCount = recordResult.value.size, error = null,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}