package com.example.mobile.data.remote

import com.example.mobile.BuildConfig
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.StateFlow

class RemoteRepositories(
    sessions: StateFlow<RemoteSession?>,
    baseUrl: String = BuildConfig.API_BASE_URL,
    language: suspend () -> AppLanguage = { AppLanguage.RUSSIAN },
    localPhoto: suspend (ownerId: String, petId: String) -> String? = { _, _ -> null },
    savePhoto: suspend (ownerId: String, petId: String, uri: String?) -> RepositoryResult<Unit> = { _, _, _ -> RepositoryResult.Success(Unit) },
    onUnauthorized: (RemoteSession) -> Unit = {},
) {
    private val operations = RemoteOperations(sessions, onUnauthorized)
    internal val api = ApiClient(baseUrl, operations, language)
    internal val transfers = RemoteTransferSource(api, operations)
    private val remotePets = RemotePetRepository(api, operations, localPhoto, savePhoto)

    val owner: OwnerRepository = RemoteOwnerRepository(api, operations)
    val pets: PetRepository = remotePets
    val medicalRecords: MedicalRecordRepository = RemoteMedicalRecordRepository(api, operations, remotePets)

    fun refresh() = operations.refresh()
}