package com.example.mobile.data.remote

import com.example.mobile.BuildConfig
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import kotlinx.coroutines.flow.StateFlow

class RemoteRepositories(
    sessions: StateFlow<RemoteSession?>,
    baseUrl: String = BuildConfig.API_BASE_URL,
    language: () -> AppLanguage = { AppLanguage.RUSSIAN },
    localPhoto: (ownerId: String, petId: String) -> String? = { _, _ -> null },
) {
    private val operations = RemoteOperations(sessions)
    private val api = ApiClient(baseUrl, operations, language)
    private val remotePets = RemotePetRepository(api, operations, localPhoto)

    val owner: OwnerRepository = RemoteOwnerRepository(api, operations)
    val pets: PetRepository = remotePets
    val medicalRecords: MedicalRecordRepository = RemoteMedicalRecordRepository(api, operations, remotePets)

    fun refresh() = operations.refresh()
}