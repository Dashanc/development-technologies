package com.example.mobile.domain.model

import java.time.LocalDate

data class Pet(
    val id: String,
    val ownerId: String,
    val name: String,
    val species: PetSpecies,
    val breed: String? = null,
    val birthDate: LocalDate? = null,
    val sex: PetSex = PetSex.UNKNOWN,
    val photoUri: String? = null,
    val extra: PetExtraData = PetExtraData(),
)

enum class PetSpecies { CAT, DOG, OTHER }

enum class PetSex { FEMALE, MALE, UNKNOWN }

data class PetExtraData(
    val coat: String? = null,
    val microchip: String? = null,
    val passportNumber: String? = null,
    val neutered: Boolean = false,
    val allergies: String? = null,
)