package com.example.mobile.data.local

import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError

internal class LocalPetRepository(private val storage: PassportStorage) : PetRepository {
    override fun observePets(ownerId: String) = storage.observe { data ->
        data.pets.filter { it.ownerId == ownerId }.map { it.toDomain() }
    }

    override fun observePet(petId: String) = storage.observe { data ->
        data.pets.find { it.id == petId }?.toDomain()
    }

    override suspend fun save(pet: Pet) = write(pet, requireExisting = false)

    override suspend fun update(pet: Pet) = write(pet, requireExisting = true)

    private suspend fun write(pet: Pet, requireExisting: Boolean) = storage.update { data ->
        if (data.owner == null || data.owner.id != pet.ownerId) {
            throw LocalDataException(RepositoryError.INVALID_REFERENCE)
        }
        val existing = data.pets.find { it.id == pet.id }
        if (requireExisting && existing == null) {
            throw LocalDataException(RepositoryError.INVALID_REFERENCE)
        }
        if (existing != null && existing.ownerId != pet.ownerId) {
            throw LocalDataException(RepositoryError.IDENTITY_CONFLICT)
        }
        val local = pet.toLocal()
        data.copy(pets = if (existing == null) data.pets + local else data.pets.map {
            if (it.id == pet.id) local else it
        })
    }

    override suspend fun delete(petId: String) = storage.update { data ->
        data.copy(
            pets = data.pets.filterNot { it.id == petId },
            records = data.records.filterNot { it.petId == petId },
        )
    }
}