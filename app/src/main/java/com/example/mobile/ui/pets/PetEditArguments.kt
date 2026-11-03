package com.example.mobile.ui.pets

import android.os.Bundle
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.domain.model.PetSex
import com.example.mobile.domain.model.PetSpecies
import com.example.mobile.presenter.pets.PetInput

internal object PetEditArguments {
    const val DRAFT = "pet_edit_draft"

    fun fromPet(pet: Pet): Bundle = fromInput(PetInput(
        name = pet.name,
        species = pet.species,
        breed = pet.breed.orEmpty(),
        birth = pet.birthDate?.let(PetText::inputDate).orEmpty(),
        sex = pet.sex,
        photoUri = pet.photoUri,
        extra = pet.extra,
    ))

    fun fromInput(input: PetInput): Bundle = Bundle().apply {
        putString("name", input.name)
        putString("species", input.species.name)
        putString("breed", input.breed)
        putString("birth", input.birth)
        putString("sex", input.sex.name)
        putString("photo", input.photoUri)
        putString("coat", input.extra.coat)
        putString("microchip", input.extra.microchip)
        putString("passport", input.extra.passportNumber)
        putBoolean("neutered", input.extra.neutered)
        putString("allergies", input.extra.allergies)
    }

    fun input(bundle: Bundle): PetInput = PetInput(
        name = bundle.getString("name").orEmpty(),
        species = PetSpecies.entries.firstOrNull { it.name == bundle.getString("species") } ?: PetSpecies.CAT,
        breed = bundle.getString("breed").orEmpty(),
        birth = bundle.getString("birth").orEmpty(),
        sex = PetSex.entries.firstOrNull { it.name == bundle.getString("sex") } ?: PetSex.UNKNOWN,
        photoUri = bundle.getString("photo"),
        extra = PetExtraData(
            coat = bundle.getString("coat"),
            microchip = bundle.getString("microchip"),
            passportNumber = bundle.getString("passport"),
            neutered = bundle.getBoolean("neutered"),
            allergies = bundle.getString("allergies"),
        ),
    )
}