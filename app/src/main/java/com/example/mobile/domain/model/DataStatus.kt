package com.example.mobile.domain.model

import com.example.mobile.domain.repository.RepositoryError

enum class DataAvailability { LOCAL, REFRESHING, READY, CACHED, UNAVAILABLE }

data class DataStatus(
    val availability: DataAvailability = DataAvailability.LOCAL,
    val updatedAt: Long? = null,
    val error: RepositoryError? = null,
)