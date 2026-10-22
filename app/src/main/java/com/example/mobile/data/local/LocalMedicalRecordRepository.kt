package com.example.mobile.data.local

import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.RepositoryError

internal class LocalMedicalRecordRepository(private val storage: PassportStorage) : MedicalRecordRepository {
    override fun observeRecords(petId: String) = storage.observe { data ->
        data.records.filter { it.petId == petId }.map { it.toDomain() }
    }

    override fun observeRecord(recordId: String) = storage.observe { data ->
        data.records.find { it.id == recordId }?.toDomain()
    }

    override suspend fun save(record: MedicalRecord) = storage.update { data ->
        if (data.pets.none { it.id == record.petId }) {
            throw LocalDataException(RepositoryError.INVALID_REFERENCE)
        }
        val local = record.toLocal()
        val existing = data.records.find { it.id == record.id }
        if (existing != null && (existing.petId != record.petId || existing::class != local::class)) {
            throw LocalDataException(RepositoryError.IDENTITY_CONFLICT)
        }
        data.copy(records = if (existing == null) data.records + local else data.records.map {
            if (it.id == record.id) local else it
        })
    }

    override suspend fun delete(recordId: String) = storage.update { data ->
        data.copy(records = data.records.filterNot { it.id == recordId })
    }
}