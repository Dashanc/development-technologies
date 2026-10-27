package com.example.mobile.domain.repository

interface PetPhotoRepository {
    suspend fun retain(uri: String): RepositoryResult<String>
}