package com.example.mobile.domain.repository

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Failure(val error: RepositoryError) : RepositoryResult<Nothing>
}

enum class RepositoryError {
    READ_FAILED,
    WRITE_FAILED,
    CORRUPT_DATA,
    UNSUPPORTED_SCHEMA,
    INVALID_DATA,
    INVALID_REFERENCE,
    IDENTITY_CONFLICT,
}