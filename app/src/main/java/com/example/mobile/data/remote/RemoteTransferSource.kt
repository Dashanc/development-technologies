package com.example.mobile.data.remote

import com.example.mobile.data.remote.dto.MedicalRecordDto
import com.example.mobile.data.remote.dto.OwnerDto
import com.example.mobile.data.remote.dto.OwnerWriteDto
import com.example.mobile.data.remote.dto.PetDto
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.serialization.json.encodeToJsonElement

internal class RemoteTransferSource(private val api: ApiClient, private val operations: RemoteOperations) {
    private suspend fun <T> run(expected: RemoteSession, call: suspend () -> T) = operations.run(expected = expected) { call() }

    suspend fun owner(session: RemoteSession) = run(session) {
        readOwner(session, api.request(session, "GET", listOf("owner")))
    }

    suspend fun saveOwner(session: RemoteSession, owner: Owner) = run(session) {
        readOwner(session, api.request(session, "PATCH", listOf("owner"),
            api.json.encodeToJsonElement(OwnerWriteDto(owner.name, owner.email, owner.phone.ifBlank { null })),
            etag = api.version(owner.revision)))
    }

    suspend fun createPet(session: RemoteSession, pet: Pet, key: String) = run(session) {
        val response = api.request(session, "POST", listOf("pets"), api.json.encodeToJsonElement(pet.toWriteDto()), key = key)
        val saved = api.decode(response, PetDto.serializer()).toDomain(null, api.revision(response))
        if (saved.ownerId != session.ownerId) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
        saved
    }

    suspend fun createRecord(session: RemoteSession, record: MedicalRecord, key: String) = run(session) {
        val location = record.location()
        val response = api.request(session, "POST", listOf("pets", record.petId, location.collection), record.toWriteJson(api.json), key = key)
        val dto = api.decode(response, MedicalRecordDto.serializer())
        if (dto.location() != location) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
        dto.toDomain(api.revision(response))
    }

    private fun readOwner(session: RemoteSession, response: ApiPayload): Owner {
        val owner = api.decode(response, OwnerDto.serializer()).toDomain(api.revision(response))
        if (owner.id != session.ownerId) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
        return owner
    }
}