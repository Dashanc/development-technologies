package com.example.mobile.presenter

import com.example.mobile.domain.model.DataAvailability
import com.example.mobile.domain.repository.DataStatusRepository
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class DataMessage { UPDATING, CACHED, UNAVAILABLE }

data class DataStatusState(
    val message: DataMessage? = null,
    val updatedAt: Long? = null,
    val error: RepositoryError? = null,
    val canRetry: Boolean = false,
)

class DataStatusPresenter(private val repository: DataStatusRepository, scope: CoroutineScope) {
    val state = repository.state.map {
        DataStatusState(message = when (it.availability) {
            DataAvailability.LOCAL, DataAvailability.READY -> null
            DataAvailability.REFRESHING -> DataMessage.UPDATING
            DataAvailability.CACHED -> DataMessage.CACHED
            DataAvailability.UNAVAILABLE -> DataMessage.UNAVAILABLE
        }, updatedAt = it.updatedAt, error = it.error,
            canRetry = it.availability == DataAvailability.CACHED || it.availability == DataAvailability.UNAVAILABLE)
    }.stateIn(scope, SharingStarted.Eagerly, DataStatusState())

    fun retry() = repository.refresh()
}