package com.example.mobile.presenter.pets

import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.domain.model.PetSex
import com.example.mobile.domain.model.PetSpecies
import java.time.DateTimeException
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.LocalDate
import java.time.Period

data class PetInput(
    val name: String = "",
    val species: PetSpecies = PetSpecies.CAT,
    val breed: String = "",
    val birth: String = "",
    val sex: PetSex = PetSex.UNKNOWN,
    val photoUri: String? = null,
    val extra: PetExtraData = PetExtraData(),
)

enum class PetFieldError { NAME_REQUIRED, BIRTH_REQUIRED, BIRTH_INVALID, BIRTH_FUTURE, SEX_REQUIRED, MICROCHIP_INVALID }

data class PetInputErrors(
    val name: PetFieldError? = null,
    val birth: PetFieldError? = null,
    val sex: PetFieldError? = null,
    val microchip: PetFieldError? = null,
) {
    val isEmpty get() = name == null && birth == null && sex == null && microchip == null
}

data class PetAge(val years: Int, val months: Int, val days: Int)

data class PetCard(val pet: Pet, val age: PetAge?, val lastWeightKg: Double? = null)

internal fun Pet.toCard(today: LocalDate = LocalDate.now()): PetCard {
    val age = birthDate?.takeUnless { it.isAfter(today) }?.let {
        Period.between(it, today).let { period -> PetAge(period.years, period.months, period.days) }
    }
    return PetCard(this, age)
}

internal object PetFormValidator {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val years = Regex("^([0-9]{1,3})\\s*(год|года|лет|г\\.?|years?|yrs?|y|жыл)?$", RegexOption.IGNORE_CASE)
    private val months = Regex("^([0-9]{1,4})\\s*(месяц|месяца|месяцев|мес\\.?|months?|mos?|ай)$", RegexOption.IGNORE_CASE)

    fun birthDate(value: String, today: LocalDate): LocalDate? {
        val input = value.trim()
        return try {
            when {
                years.matches(input) -> today.minusYears(years.matchEntire(input)!!.groupValues[1].toLong())
                months.matches(input) -> today.minusMonths(months.matchEntire(input)!!.groupValues[1].toLong())
                else -> LocalDate.parse(input, dateFormat)
            }
        } catch (_: DateTimeException) {
            null
        }
    }

    fun microchip(value: String?): PetFieldError? = if (value.isNullOrBlank() ||
        value.trim().let { it.length == 15 && it.all { digit -> digit in '0'..'9' } }
    ) null else PetFieldError.MICROCHIP_INVALID

    fun validate(input: PetInput, today: LocalDate): PetInputErrors {
        val birth = birthDate(input.birth, today)
        return PetInputErrors(
            name = if (input.name.isBlank()) PetFieldError.NAME_REQUIRED else null,
            birth = when {
                input.birth.isBlank() -> PetFieldError.BIRTH_REQUIRED
                birth == null -> PetFieldError.BIRTH_INVALID
                birth.isAfter(today) -> PetFieldError.BIRTH_FUTURE
                else -> null
            },
            sex = if (input.sex == PetSex.UNKNOWN) PetFieldError.SEX_REQUIRED else null,
            microchip = microchip(input.extra.microchip),
        )
    }

    fun pet(id: String, ownerId: String, input: PetInput, today: LocalDate) = Pet(
        id = id,
        ownerId = ownerId,
        name = input.name.trim(),
        species = input.species,
        breed = input.breed.trim().takeIf(String::isNotEmpty),
        birthDate = requireNotNull(birthDate(input.birth, today)),
        sex = input.sex,
        photoUri = input.photoUri,
        extra = input.extra.let { extra ->
            fun String?.clean() = this?.trim()?.takeIf(String::isNotEmpty)
            extra.copy(
                coat = extra.coat.clean(), microchip = extra.microchip.clean(),
                passportNumber = extra.passportNumber.clean(), allergies = extra.allergies.clean(),
            )
        },
    )
}