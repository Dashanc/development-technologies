package com.example.mobile.data.local.model

import kotlinx.serialization.Serializable

@Serializable
internal data class LocalPassportData(
    val schemaVersion: Int = 1,
    val owner: LocalOwner? = null,
    val pets: List<LocalPet> = emptyList(),
    val records: List<LocalMedicalRecord> = emptyList(),
)

@Serializable
internal data class LocalOwner(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
)

@Serializable
internal data class LocalPet(
    val id: String,
    val ownerId: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val birthDate: String? = null,
    val sex: String = "unknown",
    val photoUri: String? = null,
    val extra: LocalPetExtraData = LocalPetExtraData(),
)

@Serializable
internal data class LocalPetExtraData(
    val coat: String? = null,
    val microchip: String? = null,
    val passportNumber: String? = null,
    val neutered: Boolean = false,
    val allergies: String? = null,
)