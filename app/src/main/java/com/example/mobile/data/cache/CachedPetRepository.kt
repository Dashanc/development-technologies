package com.example.mobile.data.cache

import com.example.mobile.data.local.model.CachedPassport
import com.example.mobile.data.local.toCache
import com.example.mobile.data.local.toDomain
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.repository.PetRepository
import kotlinx.coroutines.flow.first

internal fun CachedPassport.withoutPet(id: String): CachedPassport {
    val removed = records.filterValues { it.value.petId == id }.keys
    return copy(pets = pets - id, records = records - removed, recordsLoaded = recordsLoaded - id,
        missingPets = missingPets + id, missingRecords = missingRecords + removed)
}

internal class CachedPetRepository(private val remote: PetRepository, private val cache: CacheCoordinator) : PetRepository {
    override fun observePets(ownerId: String) = cache.observe(expectedOwner = ownerId) { data, photos ->
        if (!data.petsLoaded) null
        else CacheValue(data.pets.values.sortedBy { it.value.id }.map { it.toDomain(photos[it.value.id]) })
    }

    override fun observePet(petId: String) = cache.detail<Pet?>(
        read = { remote.observePet(petId).first() },
        select = { data, photos -> when {
            petId in data.pets -> CacheValue(data.pets.getValue(petId).toDomain(photos[petId]))
            petId in data.missingPets || data.petsLoaded -> CacheValue(null)
            else -> null
        } },
        save = { data, pet -> if (pet == null) data.withoutPet(petId)
            else data.copy(pets = data.pets + (pet.id to pet.toCache()), missingPets = data.missingPets - pet.id) },
    )

    override suspend fun save(pet: Pet) = cache.write({ remote.save(pet) }, ::savePet)
    override suspend fun update(pet: Pet) = cache.write({ remote.update(pet) }, ::savePet)
    override suspend fun delete(petId: String, revision: String?) = cache.write({ remote.delete(petId, revision) }) { data, _ -> data.withoutPet(petId) }

    private fun savePet(data: CachedPassport, pet: Pet) = data.copy(
        pets = data.pets + (pet.id to pet.toCache()), missingPets = data.missingPets - pet.id,
    )
}