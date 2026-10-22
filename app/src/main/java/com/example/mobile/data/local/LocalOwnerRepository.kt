package com.example.mobile.data.local

import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError

internal class LocalOwnerRepository(private val storage: PassportStorage) : OwnerRepository {
    override fun observeOwner() = storage.observe { it.owner?.toDomain() }

    override suspend fun save(owner: Owner) = storage.update { data ->
        if (data.owner != null && data.owner.id != owner.id) {
            throw LocalDataException(RepositoryError.IDENTITY_CONFLICT)
        }
        data.copy(owner = owner.toLocal())
    }
}