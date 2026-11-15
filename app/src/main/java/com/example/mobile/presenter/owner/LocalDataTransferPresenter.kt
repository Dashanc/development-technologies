package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.AccountMode
import com.example.mobile.domain.repository.AuthenticationRepository
import com.example.mobile.domain.repository.LocalDataTransferPreview
import com.example.mobile.domain.repository.LocalDataTransferProgress
import com.example.mobile.domain.repository.LocalDataTransferRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class LocalDataTransferState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val preview: LocalDataTransferPreview? = null,
    val consent: Boolean = false,
    val includeOwner: Boolean = false,
    val progress: LocalDataTransferProgress = LocalDataTransferProgress(),
    val error: RepositoryError? = null,
    val fieldErrors: List<String> = emptyList(),
)

class LocalDataTransferPresenter(
    private val transfers: LocalDataTransferRepository,
    private val authentication: AuthenticationRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(LocalDataTransferState())
    val state = mutableState.asStateFlow()
    private var work: Job? = null

    init {
        scope.launch {
            authentication.session.collectLatest { account ->
                work?.cancel()
                mutableState.value = LocalDataTransferState()
                if (account.mode == AccountMode.SERVER) retry()
            }
        }
    }

    fun retry() {
        if (state.value.busy || state.value.loading || authentication.session.value.mode != AccountMode.SERVER) return
        mutableState.value = LocalDataTransferState(visible = true, loading = true)
        work = scope.launch {
            mutableState.value = when (val result = transfers.preview()) {
                is RepositoryResult.Failure -> LocalDataTransferState(visible = true, error = result.error)
                is RepositoryResult.Success -> LocalDataTransferState(
                    visible = result.value != null, preview = result.value, includeOwner = result.value?.includeOwner == true,
                    progress = result.value?.progress ?: LocalDataTransferProgress(),
                )
            }
        }
    }

    fun setConsent(accepted: Boolean) {
        if (!state.value.busy) mutableState.value = state.value.copy(consent = accepted)
    }

    fun setIncludeOwner(included: Boolean) {
        if (!state.value.busy && state.value.preview?.started == false) {
            mutableState.value = state.value.copy(includeOwner = included, consent = false)
        }
    }

    fun transfer() {
        val current = state.value
        val preview = current.preview ?: return
        if (!current.consent || current.busy || current.loading || preview.completed) return
        mutableState.value = current.copy(busy = true, error = null, fieldErrors = emptyList())
        work = scope.launch {
            val result = transfers.transfer(preview.id, current.includeOwner) { progress ->
                mutableState.value = state.value.copy(progress = progress)
            }
            when (result) {
                is RepositoryResult.Success -> mutableState.value = state.value.copy(
                    busy = false, consent = false, preview = preview.copy(started = true, completed = true),
                )
                is RepositoryResult.Failure -> {
                    val refreshed = (transfers.preview() as? RepositoryResult.Success)?.value
                    mutableState.value = state.value.copy(busy = false, consent = false, preview = refreshed ?: preview,
                        includeOwner = refreshed?.includeOwner ?: current.includeOwner,
                        error = result.error, fieldErrors = result.fieldErrors.values.distinct())
                }
            }
        }
    }
}