package com.example.mobile.data.remote

import com.example.mobile.data.remote.dto.PetDto
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.encodeToJsonElement

internal class RemotePetRepository(
    private val api: ApiClient,
    private val operations: RemoteOperations,
    private val localPhoto: (ownerId: String, petId: String) -> String?,
) : PetRepository {
    override fun observePets(ownerId: String) = operations.observe { session ->
        if (session.ownerId != ownerId) throw RemoteFailure(RepositoryError.ACCESS_DENIED)
        list(session)
    }

    override fun observePet(petId: String) = operations.observe { session ->
        validateId(petId)
        try {
            read(session, api.request(session, "GET", listOf("pets", petId)), petId)
        } catch (error: RemoteFailure) {
            if (error.status != 404) throw error
            null
        }
    }

    override suspend fun save(pet: Pet) = write(pet, requireExisting = false)

    override suspend fun update(pet: Pet) = write(pet, requireExisting = true)

    override suspend fun delete(petId: String, revision: String?) = operations.run(write = true) { session ->
        validateId(petId)
        api.request(session, "DELETE", listOf("pets", petId), etag = api.version(revision))
        session.metadata.pets.remove(petId)
        session.metadata.photos.remove(petId)
        session.metadata.records.entries.removeIf { it.value.petId == petId }
        Unit
    }

    internal suspend fun list(session: RemoteSession): List<Pet> {
        val items = api.decode(api.request(session, "GET", listOf("pets")), ListSerializer(PetDto.serializer()))
        return items.map { dto ->
            val pet = dto.toDomain(photo(session, dto.id), null)
            if (pet.ownerId != session.ownerId) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
            session.metadata.pets.add(pet.id)
            pet
        }
    }

    private suspend fun write(pet: Pet, requireExisting: Boolean) = operations.run(write = true) { session ->
        validateId(pet.id)
        if (pet.ownerId != session.ownerId) throw RemoteFailure(RepositoryError.IDENTITY_CONFLICT)
        val path = listOf("pets", pet.id)
        val response = when {
            pet.revision != null -> api.request(
                session, "PATCH", path, api.json.encodeToJsonElement(pet.toWriteDto()), etag = api.version(pet.revision),
            )
            requireExisting || session.metadata.pets.contains(pet.id) -> throw RemoteFailure(RepositoryError.VERSION_CONFLICT)
            else -> {
                try {
                    api.request(session, "GET", path)
                    throw RemoteFailure(RepositoryError.VERSION_CONFLICT)
                } catch (error: RemoteFailure) {
                    if (error.status != 404) throw error
                }
                api.request(session, "POST", listOf("pets"), api.json.encodeToJsonElement(pet.toWriteDto()), key = "pet:${pet.id}")
            }
        }
        val result = read(session, response, if (pet.revision != null) pet.id else null)
        session.metadata.photos[result.id] = pet.photoUri.orEmpty()
        result.copy(photoUri = pet.photoUri)
    }

    private fun read(session: RemoteSession, response: ApiPayload, expectedId: String?): Pet {
        val dto = api.decode(response, PetDto.serializer())
        val pet = dto.toDomain(photo(session, dto.id), api.revision(response))
        if (pet.ownerId != session.ownerId || (expectedId != null && pet.id != expectedId)) {
            throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
        }
        session.metadata.pets.add(pet.id)
        return pet
    }

    private fun photo(session: RemoteSession, petId: String): String? =
        if (session.metadata.photos.containsKey(petId)) session.metadata.photos[petId]?.takeIf { it.isNotEmpty() }
        else localPhoto(session.ownerId, petId)
}