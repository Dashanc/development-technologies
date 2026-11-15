package com.example.mobile.domain.repository

interface PetPhotoRepository {
    suspend fun retain(uri: String): RepositoryResult<String>
    suspend fun releaseIfUnused(uri: String): RepositoryResult<Unit>
}