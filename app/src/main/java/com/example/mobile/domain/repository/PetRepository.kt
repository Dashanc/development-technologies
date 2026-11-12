package com.example.mobile.domain.repository

import com.example.mobile.domain.model.Pet
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun observePets(ownerId: String): Flow<RepositoryResult<List<Pet>>>
    fun observePet(petId: String): Flow<RepositoryResult<Pet?>>
    suspend fun save(pet: Pet): RepositoryResult<Pet>
    suspend fun update(pet: Pet): RepositoryResult<Pet>

    suspend fun delete(petId: String, revision: String? = null): RepositoryResult<Unit>
}