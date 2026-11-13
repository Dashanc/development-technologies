package com.example.mobile.domain.repository

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Failure(
        val error: RepositoryError,
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : RepositoryResult<Nothing>
}

inline fun <T, R> RepositoryResult<T>.map(transform: (T) -> R): RepositoryResult<R> = when (this) {
    is RepositoryResult.Success -> RepositoryResult.Success(transform(value))
    is RepositoryResult.Failure -> this
}

enum class RepositoryError {
    READ_FAILED,
    WRITE_FAILED,
    CORRUPT_DATA,
    UNSUPPORTED_SCHEMA,
    INVALID_DATA,
    INVALID_REFERENCE,
    IDENTITY_CONFLICT,
    NETWORK_UNAVAILABLE,
    REQUEST_TIMEOUT,
    AUTHENTICATION_REQUIRED,
    ACCESS_DENIED,
    CONFLICT,
    VERSION_CONFLICT,
    RATE_LIMITED,
    SERVER_UNAVAILABLE,
    INVALID_RESPONSE,
    NOT_CONFIGURED,
    SECURE_STORAGE_FAILED,
    INVALID_CREDENTIALS,
}