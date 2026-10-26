package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OwnerProfileState(
    val owner: Owner? = null,
    val loading: Boolean = true,
    val error: RepositoryError? = null,
)

class OwnerProfilePresenter(private val owners: OwnerRepository, private val scope: CoroutineScope) {
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
            owners.observeOwner().collect { result ->
                mutableState.value = when (result) {
                    is RepositoryResult.Failure -> OwnerProfileState(loading = false, error = result.error)
                    is RepositoryResult.Success -> OwnerProfileState(owner = result.value, loading = false)
                }
            }
        }
    }
}