package com.example.mobile.data.local.model

import kotlinx.serialization.Serializable

@Serializable
internal data class CachedPassport(
    val schemaVersion: Int = 1,
    val server: String,
    val ownerId: String,
    val owner: CachedOwner? = null,
    val pets: Map<String, CachedPet> = emptyMap(),
    val records: Map<String, CachedRecord> = emptyMap(),
    val petsLoaded: Boolean = false,
    val recordsLoaded: Set<String> = emptySet(),
    val missingPets: Set<String> = emptySet(),
    val missingRecords: Set<String> = emptySet(),
    val updatedAt: Long? = null,
)

@Serializable
internal data class CachedOwner(val value: LocalOwner, val revision: String?)

@Serializable
internal data class CachedPet(val value: LocalPet, val revision: String?)

@Serializable
internal data class CachedRecord(val value: LocalMedicalRecord, val revision: String?)