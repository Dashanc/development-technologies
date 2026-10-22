package com.example.mobile.domain.repository

import com.example.mobile.domain.model.MedicalRecord
import kotlinx.coroutines.flow.Flow

interface MedicalRecordRepository {
    fun observeRecords(petId: String): Flow<RepositoryResult<List<MedicalRecord>>>
    fun observeRecord(recordId: String): Flow<RepositoryResult<MedicalRecord?>>

    suspend fun save(record: MedicalRecord): RepositoryResult<Unit>
    suspend fun delete(recordId: String): RepositoryResult<Unit>
}