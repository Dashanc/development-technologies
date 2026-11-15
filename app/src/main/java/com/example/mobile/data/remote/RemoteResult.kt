package com.example.mobile.data.remote

import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.io.IOException
import java.io.InterruptedIOException
import java.time.DateTimeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class RemoteFailure(
    val error: RepositoryError,
    val fields: Map<String, String> = emptyMap(),
    val status: Int? = null,
) : RuntimeException()

internal class RemoteOperations(
    private val sessions: StateFlow<RemoteSession?>,
    private val unauthorized: (RemoteSession) -> Unit = {},
) {
    private val revision = MutableStateFlow(0L)

    fun check(session: RemoteSession) {
        if (sessions.value !== session) throw RemoteFailure(RepositoryError.AUTHENTICATION_REQUIRED)
    }

    fun refresh() = revision.update { it + 1 }

    suspend fun <T> run(write: Boolean = false, expected: RemoteSession? = null, block: suspend (RemoteSession) -> T): RepositoryResult<T> {
        val session = expected ?: sessions.value
        val result = remoteResult {
            if (session == null) throw RemoteFailure(RepositoryError.AUTHENTICATION_REQUIRED)
            check(session)
            val result = block(session)
            check(session)
            if (write) refresh()
            result
        }
        if (session != null && result is RepositoryResult.Failure && result.error == RepositoryError.AUTHENTICATION_REQUIRED) unauthorized(session)
        return result
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun <T> observe(read: suspend (RemoteSession) -> T): Flow<RepositoryResult<T>> =
        combine(sessions, revision) { session, _ -> session }
            .mapLatest { run(block = read) }
            .distinctUntilChanged()
}

internal suspend fun <T> remoteResult(block: suspend () -> T): RepositoryResult<T> = try {
    withContext(Dispatchers.Default) { RepositoryResult.Success(block()) }
} catch (error: CancellationException) {
    throw error
} catch (error: RemoteFailure) {
    RepositoryResult.Failure(error.error, error.fields)
} catch (_: InterruptedIOException) {
    RepositoryResult.Failure(RepositoryError.REQUEST_TIMEOUT)
} catch (_: IOException) {
    RepositoryResult.Failure(RepositoryError.NETWORK_UNAVAILABLE)
} catch (_: IllegalArgumentException) {
    RepositoryResult.Failure(RepositoryError.INVALID_RESPONSE)
} catch (_: DateTimeException) {
    RepositoryResult.Failure(RepositoryError.INVALID_RESPONSE)
}