package com.example.mobile.data.remote

import com.example.mobile.data.remote.dto.MedicalRecordDto
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.serialization.builtins.ListSerializer

internal class RemoteMedicalRecordRepository(
    private val api: ApiClient,
    private val operations: RemoteOperations,
    private val pets: RemotePetRepository,
) : MedicalRecordRepository {
    override fun observeRecords(petId: String) = operations.observe { session ->
        validateId(petId)
        history(session, petId)
    }

    override fun observeRecord(recordId: String) = operations.observe { session ->
        validateId(recordId)
        val location = locate(session, recordId)
        if (location == null) null else try {
            read(session, api.request(session, "GET", location.path(recordId)), location, recordId)
        } catch (error: RemoteFailure) {
            if (error.status != 404) throw error
            session.metadata.records.remove(recordId)
            null
        }
    }

    override suspend fun save(record: MedicalRecord) = operations.run(write = true) { session ->
        validateId(record.id)
        validateId(record.petId)
        val location = record.location()
        val known = session.metadata.records[record.id]
        if (known != null && known != location) throw RemoteFailure(RepositoryError.IDENTITY_CONFLICT)
        val body = record.toWriteJson(api.json)
        val response = if (record.revision != null) {
            api.request(session, "PATCH", location.path(record.id), body, etag = api.version(record.revision))
        } else {
            if (known != null) throw RemoteFailure(RepositoryError.VERSION_CONFLICT)
            try {
                api.request(session, "GET", location.path(record.id))
                throw RemoteFailure(RepositoryError.VERSION_CONFLICT)
            } catch (error: RemoteFailure) {
                if (error.status != 404) throw error
            }
            api.request(session, "POST", listOf("pets", record.petId, location.collection), body, key = "${location.collection}:${record.id}")
        }
        read(session, response, location, record.id.takeIf { record.revision != null })
    }

    override suspend fun delete(recordId: String, revision: String?) = operations.run(write = true) { session ->
        validateId(recordId)
        val location = locate(session, recordId) ?: throw RemoteFailure(RepositoryError.INVALID_REFERENCE)
        api.request(session, "DELETE", location.path(recordId), etag = api.version(revision))
        session.metadata.records.remove(recordId)
        Unit
    }

    private suspend fun history(session: RemoteSession, petId: String): List<MedicalRecord> {
        val response = api.request(session, "GET", listOf("pets", petId, "records"))
        return api.decode(response, ListSerializer(MedicalRecordDto.serializer())).map { dto ->
            if (dto.petId != petId) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
            val record = dto.toDomain(null)
            session.metadata.records[record.id] = dto.location()
            record
        }
    }

    private suspend fun locate(session: RemoteSession, id: String): RecordLocation? {
        session.metadata.records[id]?.let { return it }
        for (pet in pets.list(session)) {
            history(session, pet.id)
            session.metadata.records[id]?.let { return it }
        }
        return null
    }

    private fun read(session: RemoteSession, response: ApiPayload, location: RecordLocation, expectedId: String?): MedicalRecord {
        val dto = api.decode(response, MedicalRecordDto.serializer())
        val record = dto.toDomain(api.revision(response))
        if (dto.location() != location || (expectedId != null && record.id != expectedId)) {
            throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
        }
        session.metadata.records[record.id] = location
        return record
    }
}