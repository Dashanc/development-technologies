package com.example.mobile.data.cache

import com.example.mobile.data.local.toCache
import com.example.mobile.data.local.toDomain
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.OwnerRepository

internal class CachedOwnerRepository(private val remote: OwnerRepository, private val cache: CacheCoordinator) : OwnerRepository {
    override fun observeOwner() = cache.observe<Owner?> { data, _ -> data.owner?.let { CacheValue(it.toDomain()) } }
    override suspend fun save(owner: Owner) = cache.write({ remote.save(owner) }) { data, saved -> data.copy(owner = saved.toCache()) }
}