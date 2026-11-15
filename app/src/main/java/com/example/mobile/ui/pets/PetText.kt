package com.example.mobile.ui.pets

import android.content.Context
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.PetSex
import com.example.mobile.domain.model.PetSpecies
import com.example.mobile.presenter.pets.PetAge
import com.example.mobile.presenter.pets.PetCard
import com.example.mobile.R
import java.time.format.DateTimeFormatter
import java.time.LocalDate

internal class PetText(private val context: Context) {
    fun species(species: PetSpecies) = context.getString(when (species) {
        PetSpecies.CAT -> R.string.cat
        PetSpecies.DOG -> R.string.dog
        PetSpecies.OTHER -> R.string.other
    })

    fun age(age: PetAge?): String = when {
        age == null -> context.getString(R.string.pet_age_unknown)
        age.years > 0 -> context.resources.getQuantityString(R.plurals.pet_years, age.years, age.years)
        age.months > 0 -> context.resources.getQuantityString(R.plurals.pet_months, age.months, age.months)
        else -> context.resources.getQuantityString(R.plurals.pet_days, age.days, age.days)
    }

    fun description(card: PetCard): String = join(breed(card.pet), age(card.age))

    fun breed(pet: Pet): String = if (pet.breed.isNullOrBlank()) species(pet.species) else join(species(pet.species), pet.breed)

    fun extra(pet: Pet): String {
        val sex = context.getString(when (pet.sex) {
            PetSex.MALE -> R.string.male
            PetSex.FEMALE -> R.string.female
            PetSex.UNKNOWN -> R.string.pet_unspecified
        })
        val values = listOf(
            R.string.sex to sex,
            R.string.coat to pet.extra.coat,
            R.string.microchip to pet.extra.microchip,
            R.string.passport to pet.extra.passportNumber,
            R.string.neutered to context.getString(if (pet.extra.neutered) R.string.pet_yes else R.string.pet_no),
            R.string.allergies to pet.extra.allergies,
        )
        return values.joinToString("\n") { (label, value) ->
            context.getString(R.string.pet_detail_line, context.getString(label), value ?: context.getString(R.string.pet_unspecified))
        }
    }

    private fun join(first: String, second: String) = context.getString(R.string.pet_detail_join, first, second)

    companion object {
        fun inputDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu"))
    }
}