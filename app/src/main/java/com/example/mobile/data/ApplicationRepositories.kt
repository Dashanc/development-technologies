package com.example.mobile.data

import android.content.Context
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.data.local.SecureSessionStorage
import com.example.mobile.data.remote.RemoteFailure
import com.example.mobile.data.remote.RemoteRepositories
import com.example.mobile.data.remote.RemoteSession
import com.example.mobile.data.remote.ServerAuthenticationRepository
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.repository.AuthenticationRepository
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
    private val serverAuthentication: ServerAuthenticationRepository = ServerAuthenticationRepository(remote.api, sessions, SecureSessionStorage(context), local.owner, local.accountMode) {
        val reminderResult = clearReminders()
        if (reminderResult is RepositoryResult.Failure) throw RemoteFailure(reminderResult.error)
        accountLock.withLock {
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
    private val source = AccountSource(authentication.session)
    val owner = AccountOwnerRepository(source, local.owner, remote.owner)
    val pets = AccountPetRepository(source, local.pets, remote.pets)
    val medicalRecords = AccountMedicalRecordRepository(source, local.medicalRecords, remote.medicalRecords)
}