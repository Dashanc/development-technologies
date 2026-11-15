package com.example.mobile.data.cache

import com.example.mobile.data.local.AccountCacheStorage
import com.example.mobile.data.local.AccountPhotoStorage
import com.example.mobile.data.local.model.CachedPassport
import com.example.mobile.data.local.toCache
import com.example.mobile.data.remote.NetworkMonitor
import com.example.mobile.data.remote.RemoteRepositories
import com.example.mobile.data.remote.RemoteSession
import com.example.mobile.domain.model.AccountMode
import com.example.mobile.domain.model.AccountSession
import com.example.mobile.domain.model.DataAvailability
import com.example.mobile.domain.model.DataStatus
import com.example.mobile.domain.repository.DataStatusRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CacheValue<T>(val value: T, val error: RepositoryError? = null)

internal fun RepositoryError.allowsCachedRead() = this in setOf(
    RepositoryError.NETWORK_UNAVAILABLE, RepositoryError.REQUEST_TIMEOUT,
    RepositoryError.SERVER_UNAVAILABLE, RepositoryError.RATE_LIMITED,
)

internal class CacheCoordinator(
    private val storage: AccountCacheStorage,
    private val photos: AccountPhotoStorage,
    private val sessions: StateFlow<RemoteSession?>,
    private val accounts: StateFlow<AccountSession>,
    private val network: NetworkMonitor,
    private val remote: RemoteRepositories,
    private val server: String,
    scope: CoroutineScope,
) : DataStatusRepository {
    private val requests = MutableStateFlow(0L)
    private val mutableState = MutableStateFlow(DataStatus())
    override val state = mutableState.asStateFlow()
    private val remoteLock = Mutex()

    init {
        scope.launch {
            combine(accounts, network.state, requests) { account, connection, _ -> account to connection }
                .collectLatest { (account, connection) ->
                    val session = sessions.value
                    if (account.mode != AccountMode.SERVER || session == null) {
                        mutableState.value = DataStatus()
                    } else if (!connection.available) {
                        publish(session, RepositoryError.NETWORK_UNAVAILABLE)
                    } else remoteLock.withLock {
                        if (active(session)) synchronize(session)
                    }
                }
        }
    }

    override fun refresh() = requests.update { it + 1 }

    private fun active(session: RemoteSession) = sessions.value === session && accounts.value.mode == AccountMode.SERVER

    private suspend fun update(session: RemoteSession, change: (CachedPassport) -> CachedPassport) =
        storage.update(session.ownerId, server, { active(session) }, change)

    private suspend fun publish(session: RemoteSession, error: RepositoryError? = null, refreshing: Boolean = false) {
        val stored = storage.read(session.ownerId, server)
        if (!active(session)) return
        val data = (stored as? RepositoryResult.Success)?.value
        val problem = (stored as? RepositoryResult.Failure)?.error ?: error
        mutableState.value = DataStatus(
            availability = when {
                refreshing -> DataAvailability.REFRESHING
                problem == null -> DataAvailability.READY
                data?.owner != null && problem.allowsCachedRead() -> DataAvailability.CACHED
                else -> DataAvailability.UNAVAILABLE
            },
            updatedAt = data?.updatedAt, error = problem,
        )
    }

    private suspend fun synchronize(session: RemoteSession) {
        publish(session, refreshing = true)
        val ownerResult = remote.owner.observeOwner().first()
        val owner = (ownerResult as? RepositoryResult.Success)?.value
        if (owner == null) {
            publish(session, (ownerResult as? RepositoryResult.Failure)?.error ?: RepositoryError.INVALID_RESPONSE)
            return
        }
        val ownerSaved = update(session) { it.copy(owner = owner.toCache()) }
        if (ownerSaved is RepositoryResult.Failure) { publish(session, ownerSaved.error); return }
        val petsResult = remote.pets.observePets(session.ownerId).first()
        if (petsResult is RepositoryResult.Failure) { publish(session, petsResult.error); return }
        val pets = (petsResult as RepositoryResult.Success).value
        val petIds = pets.map { it.id }.toSet()
        val petsSaved = update(session) { current ->
            val removedRecords = current.records.filterValues { it.value.petId !in petIds }.keys
            current.copy(pets = pets.filter { it.id in current.pets }.associate { it.id to it.toCache() },
                records = current.records - removedRecords, recordsLoaded = current.recordsLoaded.intersect(petIds),
                missingPets = current.missingPets + (current.pets.keys - petIds) - petIds,
                missingRecords = current.missingRecords + removedRecords)
        }
        if (petsSaved is RepositoryResult.Failure) { publish(session, petsSaved.error); return }
        for (pet in pets) {
            when (val result = remote.medicalRecords.observeRecords(pet.id).first()) {
                is RepositoryResult.Failure -> { publish(session, result.error); return }
                is RepositoryResult.Success -> {
                    val recordsSaved = update(session) { current ->
                        val previous = current.records.filterValues { it.value.petId == pet.id }.keys
                        val next = result.value.associate { it.id to it.toCache() }
                        current.copy(pets = current.pets + (pet.id to pet.toCache()), missingPets = current.missingPets - pet.id,
                            records = current.records - previous + next, recordsLoaded = current.recordsLoaded + pet.id,
                            missingRecords = current.missingRecords + (previous - next.keys) - next.keys)
                    }
                    if (recordsSaved is RepositoryResult.Failure) { publish(session, recordsSaved.error); return }
                }
            }
        }
        val result = update(session) { it.copy(petsLoaded = true, updatedAt = Instant.now().toEpochMilli()) }
        publish(session, (result as? RepositoryResult.Failure)?.error)
    }

    fun <T> observe(expectedOwner: String? = null, select: (CachedPassport, Map<String, String>) -> CacheValue<T>?): Flow<RepositoryResult<T>> = flow {
        val session = sessions.value
        if (session == null || !active(session)) {
            emit(RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED))
            return@flow
        }
        if (expectedOwner != null && expectedOwner != session.ownerId) {
            emit(RepositoryResult.Failure(RepositoryError.ACCESS_DENIED))
            return@flow
        }
        val initial = storage.read(session.ownerId, server)
        val initialData = (initial as? RepositoryResult.Success)?.value
        if ((initialData == null || select(initialData, emptyMap()) == null) && state.value.availability != DataAvailability.REFRESHING) refresh()
        emitAll(combine(storage.observe(session.ownerId, server), state) { stored, status -> stored to status }
            .mapNotNull { (stored, status) ->
                if (!active(session)) return@mapNotNull RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
                if (stored is RepositoryResult.Failure) return@mapNotNull stored
                if (status.error != null && !status.error.allowsCachedRead()) return@mapNotNull RepositoryResult.Failure(status.error)
                val data = (stored as RepositoryResult.Success).value
                val photoResult = photos.read()
                if (photoResult is RepositoryResult.Failure) return@mapNotNull photoResult
                val photoData = (photoResult as RepositoryResult.Success).value
                val localPhotos = photoData.photos.takeIf { photoData.ownerId == session.ownerId }.orEmpty()
                val value = data?.let { select(it, localPhotos) }
                when {
                    value?.error != null -> RepositoryResult.Failure(value.error)
                    value != null -> RepositoryResult.Success(value.value)
                    status.error != null -> RepositoryResult.Failure(status.error)
                    else -> null
                }
            }.distinctUntilChanged())
    }.distinctUntilChanged()

    fun <T> detail(
        read: suspend () -> RepositoryResult<T>,
        select: (CachedPassport, Map<String, String>) -> CacheValue<T>?,
        save: (CachedPassport, T) -> CachedPassport,
    ): Flow<RepositoryResult<T>> = flow {
        val session = sessions.value
        if (session == null || !active(session)) {
            emit(RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)); return@flow
        }
        if (network.state.value.available) {
            val result = remoteLock.withLock {
                if (!active(session)) return@withLock RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
                val received = read()
                if (received is RepositoryResult.Success) {
                    val saved = update(session) { save(it, received.value) }
                    if (saved is RepositoryResult.Failure) return@withLock saved
                }
                received
            }
            when (result) {
                is RepositoryResult.Success -> {
                    emit(result)
                }
                is RepositoryResult.Failure -> {
                    if (!result.error.allowsCachedRead()) { emit(result); return@flow }
                    publish(session, result.error)
                }
            }
        } else publish(session, RepositoryError.NETWORK_UNAVAILABLE)
        emitAll(observe(select = select))
    }.distinctUntilChanged()

    suspend fun <T> write(call: suspend () -> RepositoryResult<T>, save: (CachedPassport, T) -> CachedPassport): RepositoryResult<T> {
        if (!network.state.value.available) return RepositoryResult.Failure(RepositoryError.OFFLINE_WRITE_NOT_ALLOWED)
        val session = sessions.value ?: return RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
        return remoteLock.withLock {
            if (!active(session)) return@withLock RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
            val result = call()
            if (result is RepositoryResult.Success) {
                val saved = update(session) { save(it, result.value) }
                if (saved is RepositoryResult.Failure) return@withLock saved
                refresh()
            } else if (result is RepositoryResult.Failure) {
                when {
                    result.error == RepositoryError.VERSION_CONFLICT -> synchronize(session)
                    result.error.allowsCachedRead() -> publish(session, result.error)
                }
            }
            result
        }
    }
}