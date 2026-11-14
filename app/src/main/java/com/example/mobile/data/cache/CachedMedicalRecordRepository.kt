package com.example.mobile.data.cache

import com.example.mobile.data.local.toCache
import com.example.mobile.data.local.toDomain
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.coroutines.flow.first

internal class CachedMedicalRecordRepository(private val remote: MedicalRecordRepository, private val cache: CacheCoordinator) : MedicalRecordRepository {
    override fun observeRecords(petId: String) = cache.observe { data, _ ->
        when {
            petId in data.missingPets || data.petsLoaded && petId !in data.pets -> CacheValue(emptyList(), RepositoryError.INVALID_REFERENCE)
            petId !in data.recordsLoaded -> null
            else -> CacheValue(data.records.values.filter { it.value.petId == petId }
                .map { it.toDomain() }.sortedWith(compareByDescending<MedicalRecord> { it.date }.thenByDescending { it.id }))
        }
    }

    override fun observeRecord(recordId: String) = cache.detail<MedicalRecord?>(
        read = { remote.observeRecord(recordId).first() },
        select = { data, _ -> when {
            recordId in data.records -> CacheValue(data.records.getValue(recordId).toDomain())
            recordId in data.missingRecords || data.petsLoaded && data.recordsLoaded.containsAll(data.pets.keys) -> CacheValue(null)
            else -> null
        } },
        save = { data, record -> if (record == null) data.copy(records = data.records - recordId, missingRecords = data.missingRecords + recordId)
            else data.copy(records = data.records + (record.id to record.toCache()), missingRecords = data.missingRecords - record.id,
                petsLoaded = data.petsLoaded && record.petId in data.pets) },
    )

    override suspend fun save(record: MedicalRecord) = cache.write({ remote.save(record) }) { data, saved ->
        data.copy(records = data.records + (saved.id to saved.toCache()), missingRecords = data.missingRecords - saved.id,
            petsLoaded = data.petsLoaded && saved.petId in data.pets)
    }

    override suspend fun delete(recordId: String, revision: String?) = cache.write({ remote.delete(recordId, revision) }) { data, _ ->
        data.copy(records = data.records - recordId, missingRecords = data.missingRecords + recordId)
    }
}