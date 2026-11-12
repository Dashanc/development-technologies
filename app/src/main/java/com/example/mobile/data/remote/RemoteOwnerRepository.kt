package com.example.mobile.data.remote

import com.example.mobile.data.remote.dto.OwnerDto
import com.example.mobile.data.remote.dto.OwnerWriteDto
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.serialization.json.encodeToJsonElement

internal class RemoteOwnerRepository(
    private val api: ApiClient,
    private val operations: RemoteOperations,
) : OwnerRepository {
    private val path = listOf("owner")

    override fun observeOwner() = operations.observe { session ->
        read(session, api.request(session, "GET", path))
    }

    override suspend fun save(owner: Owner) = operations.run(write = true) { session ->
        if (owner.id != session.ownerId) throw RemoteFailure(RepositoryError.IDENTITY_CONFLICT)
        val response = api.request(
            session, "PATCH", path,
            api.json.encodeToJsonElement(OwnerWriteDto(owner.name, owner.email, owner.phone.ifBlank { null })),
            etag = api.version(owner.revision),
        )
        read(session, response)
    }

    private fun read(session: RemoteSession, response: ApiPayload): Owner {
        val owner = api.decode(response, OwnerDto.serializer()).toDomain(api.revision(response))
        if (owner.id != session.ownerId) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
        return owner
    }
}