package com.example.mobile.data

import android.content.Context
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.data.local.SecureSessionStorage
import com.example.mobile.data.remote.RemoteFailure
import com.example.mobile.data.remote.RemoteRepositories
import com.example.mobile.data.remote.RemoteSession
import com.example.mobile.data.remote.ServerAuthenticationRepository
import com.example.mobile.data.remote.NetworkMonitor
import com.example.mobile.data.cache.CacheCoordinator
import com.example.mobile.data.cache.CachedOwnerRepository
import com.example.mobile.data.cache.CachedPetRepository
import com.example.mobile.data.cache.CachedMedicalRecordRepository
import com.example.mobile.data.transfer.LocalDataTransferRepositoryImpl
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.repository.AuthenticationRepository
import com.example.mobile.domain.repository.DataStatusRepository
import com.example.mobile.domain.repository.LocalDataTransferRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ApplicationRepositories(context: Context, scope: CoroutineScope, private val clearReminders: suspend () -> RepositoryResult<Unit>) {
    private val local = LocalRepositories.get(context)
    private val sessions = MutableStateFlow<RemoteSession?>(null)
    private val accountLock = Mutex()
    private val network = NetworkMonitor(context, scope)
    val settings = local.settings
    val petPhotos = local.petPhotos
    val reminderStorage = local.reminderStorage
    private val remote = RemoteRepositories(sessions, language = {
        (settings.observeSettings().first() as? RepositoryResult.Success)?.value?.language ?: AppLanguage.RUSSIAN
    },
        localPhoto = { owner, pet -> accountLock.withLock {
            when (val result = local.accountPhotos.read()) {
                is RepositoryResult.Failure -> throw RemoteFailure(result.error)
                is RepositoryResult.Success -> result.value.takeIf { it.ownerId == owner }?.photos?.get(pet)
            }
        } },
        savePhoto = { owner, pet, uri -> accountLock.withLock {
            if (sessions.value?.ownerId != owner) RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
            else local.accountPhotos.save(owner, pet, uri)
        } },
        onUnauthorized = { expired -> scope.launch { serverAuthentication.expire(expired) } },
    )
    private val serverAuthentication: ServerAuthenticationRepository = ServerAuthenticationRepository(
        remote.api, sessions, SecureSessionStorage(context), local.owner, local.accountMode, scope,
        networkAvailable = { network.state.value.available },
        hasCachedOwner = { owner, server -> when (val result = local.accountCache.read(owner, server)) {
            is RepositoryResult.Failure -> throw RemoteFailure(result.error)
            is RepositoryResult.Success -> result.value?.owner != null
        } },
    ) {
        val reminderResult = clearReminders()
        if (reminderResult is RepositoryResult.Failure) throw RemoteFailure(reminderResult.error)
        accountLock.withLock {
            when (val result = local.accountCache.clear()) {
                is RepositoryResult.Failure -> throw RemoteFailure(result.error)
                is RepositoryResult.Success -> Unit
            }
            val previous = local.accountPhotos.read()
            when (val result = local.accountPhotos.clear()) {
                is RepositoryResult.Failure -> throw RemoteFailure(result.error)
                is RepositoryResult.Success -> Unit
            }
            if (previous is RepositoryResult.Success) previous.value.photos.values.toSet().forEach { petPhotos.releaseIfUnused(it) }
        }
    }
    val authentication: AuthenticationRepository = serverAuthentication
    val localOwner = local.owner
    private val cached = CacheCoordinator(local.accountCache, local.accountPhotos, sessions, authentication.session,
        network, remote, remote.api.serverIdentity, scope)
    val dataStatus: DataStatusRepository = cached
    val localDataTransfer: LocalDataTransferRepository = LocalDataTransferRepositoryImpl(local.storage, local.transfers, remote.transfers, sessions,
        authentication.session, remote.api.serverIdentity, { network.state.value.available }, cached::refresh, ::linkTransferredPhotos)
    private val source = AccountSource(authentication.session)
    val owner = AccountOwnerRepository(source, local.owner, CachedOwnerRepository(remote.owner, cached))
    val pets = AccountPetRepository(source, local.pets, CachedPetRepository(remote.pets, cached))
    val medicalRecords = AccountMedicalRecordRepository(source, local.medicalRecords, CachedMedicalRecordRepository(remote.medicalRecords, cached))

    private suspend fun linkTransferredPhotos(session: RemoteSession, sourceOwner: String, petIds: Map<String, String>): RepositoryResult<Unit> = accountLock.withLock {
        if (sessions.value !== session) return@withLock RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
        val source = local.storage.observe { it }.first()
        if (source is RepositoryResult.Failure) return@withLock source
        val passport = (source as RepositoryResult.Success).value
        if (passport.owner?.id != sourceOwner) return@withLock RepositoryResult.Success(Unit)
        val stored = local.accountPhotos.read()
        if (stored is RepositoryResult.Failure) return@withLock stored
        val photos = (stored as RepositoryResult.Success).value
        if (photos.ownerId != null && photos.ownerId != session.ownerId) return@withLock RepositoryResult.Failure(RepositoryError.IDENTITY_CONFLICT)
        for ((localId, serverId) in petIds) {
            if (sessions.value !== session) return@withLock RepositoryResult.Failure(RepositoryError.AUTHENTICATION_REQUIRED)
            val uri = passport.pets.firstOrNull { it.id == localId }?.photoUri ?: continue
            if (serverId in photos.photos) continue
            val saved = local.accountPhotos.save(session.ownerId, serverId, uri)
            if (saved is RepositoryResult.Failure) return@withLock saved
        }
        RepositoryResult.Success(Unit)
    }
}