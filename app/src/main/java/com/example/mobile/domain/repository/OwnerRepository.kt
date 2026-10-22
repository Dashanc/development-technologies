package com.example.mobile.domain.repository

import com.example.mobile.domain.model.Owner
import kotlinx.coroutines.flow.Flow

interface OwnerRepository {
    fun observeOwner(): Flow<RepositoryResult<Owner?>>

    suspend fun save(owner: Owner): RepositoryResult<Unit>
}