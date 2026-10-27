package com.example.mobile.presenter.pets

import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PetListState(
    val pets: List<PetCard> = emptyList(),
    val loading: Boolean = true,
    val ownerMissing: Boolean = false,
    val error: RepositoryError? = null,
)

class PetListPresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
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
            owners.observeOwner().collectLatest { result ->
                when (result) {
                    is RepositoryResult.Failure -> mutableState.value = PetListState(loading = false, error = result.error)
                    is RepositoryResult.Success -> {
                        val owner = result.value
                        if (owner == null) {
                            mutableState.value = PetListState(loading = false, ownerMissing = true)
                        } else {
                            pets.observePets(owner.id).collect { petResult ->
                                mutableState.value = when (petResult) {
                                    is RepositoryResult.Failure -> PetListState(loading = false, error = petResult.error)
                                    is RepositoryResult.Success -> PetListState(petResult.value.map { it.toCard() }, loading = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}