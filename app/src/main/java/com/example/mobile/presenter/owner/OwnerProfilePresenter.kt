package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.presenter.pets.PetCard
import com.example.mobile.presenter.pets.toCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class OwnerProfileState(
    val owner: Owner? = null,
    val loading: Boolean = true,
    val error: RepositoryError? = null,
    val pets: List<PetCard> = emptyList(),
    val petsLoading: Boolean = true,
    val petError: RepositoryError? = null,
)

class OwnerProfilePresenter(
    private val owners: OwnerRepository,
    private val pets: PetRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(OwnerProfileState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        mutableState.value = state.value.copy(loading = true, error = null)
        observation = scope.launch {
            owners.observeOwner().collectLatest { result ->
                when (result) {
                    is RepositoryResult.Failure -> mutableState.value = OwnerProfileState(loading = false, error = result.error)
                    is RepositoryResult.Success -> {
                        val owner = result.value
                        mutableState.value = OwnerProfileState(owner = owner, loading = false, petsLoading = owner != null)
                        if (owner != null) pets.observePets(owner.id).collect { petResult ->
                            mutableState.value = when (petResult) {
                                is RepositoryResult.Failure -> state.value.copy(petsLoading = false, petError = petResult.error)
                                is RepositoryResult.Success -> state.value.copy(
                                    pets = petResult.value.map { it.toCard() }, petsLoading = false, petError = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}