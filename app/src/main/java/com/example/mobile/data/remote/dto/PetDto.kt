package com.example.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PetDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val birthDate: String? = null,
    val sex: String,
    val extra: PetExtraDto,
)

@Serializable
internal data class PetExtraDto(
    val coat: String? = null,
    val microchip: String? = null,
    val passportNumber: String? = null,
    val neutered: Boolean = false,
    val allergies: String? = null,
)

@Serializable
internal data class PetWriteDto(
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: String?,
    val sex: String,
    val extra: PetExtraDto,
)