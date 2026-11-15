package com.example.mobile.data.local

import com.example.mobile.data.local.model.LocalMedicalRecord
import com.example.mobile.data.local.model.LocalOwner
import com.example.mobile.data.local.model.LocalPet
import com.example.mobile.data.local.model.LocalPetExtraData
import com.example.mobile.data.local.model.LocalVaccinationRecord
import com.example.mobile.data.local.model.LocalVetVisitRecord
import com.example.mobile.data.local.model.LocalWeightRecord
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.domain.model.PetSex
import com.example.mobile.domain.model.PetSpecies
import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import java.time.LocalDate

internal fun LocalOwner.toDomain() = Owner(id, name, email, phone)

internal fun Owner.toLocal() = LocalOwner(id, name, email, phone)

internal fun LocalPet.toDomain() = Pet(
    id = id,
    ownerId = ownerId,
    name = name,
    species = when (species) {
        "cat" -> PetSpecies.CAT
        "dog" -> PetSpecies.DOG
        "other" -> PetSpecies.OTHER
        else -> throw IllegalArgumentException("Unknown species code")
    },
    breed = breed,
    birthDate = birthDate?.let(LocalDate::parse),
    sex = when (sex) {
        "female" -> PetSex.FEMALE
        "male" -> PetSex.MALE
        "unknown" -> PetSex.UNKNOWN
        else -> throw IllegalArgumentException("Unknown sex code")
    },
    photoUri = photoUri,
    extra = PetExtraData(extra.coat, extra.microchip, extra.passportNumber, extra.neutered, extra.allergies),
)

internal fun Pet.toLocal() = LocalPet(
    id = id,
    ownerId = ownerId,
    name = name,
    species = when (species) {
        PetSpecies.CAT -> "cat"
        PetSpecies.DOG -> "dog"
        PetSpecies.OTHER -> "other"
    },
    breed = breed,
    birthDate = birthDate?.toString(),
    sex = when (sex) {
        PetSex.FEMALE -> "female"
        PetSex.MALE -> "male"
        PetSex.UNKNOWN -> "unknown"
    },
    photoUri = photoUri,
    extra = LocalPetExtraData(extra.coat, extra.microchip, extra.passportNumber, extra.neutered, extra.allergies),
)

internal fun LocalMedicalRecord.toDomain(): MedicalRecord = when (this) {
    is LocalWeightRecord -> WeightRecord(id, petId, LocalDate.parse(date), weightKg, note)
    is LocalVaccinationRecord -> VaccinationRecord(
        id = id,
        petId = petId,
        date = LocalDate.parse(date),
        vaccine = vaccine,
        nextVaccinationDate = nextVaccinationDate?.let(LocalDate::parse),
        clinic = clinic,
        veterinarian = veterinarian,
        note = note,
    )
    is LocalVetVisitRecord -> VetVisitRecord(
        id = id,
        petId = petId,
        date = LocalDate.parse(date),
        reason = reason,
        clinic = clinic,
        veterinarian = veterinarian,
        diagnosis = diagnosis,
        recommendations = recommendations,
        appointmentDate = appointmentDate?.let(LocalDate::parse),
        note = note,
    )
}

internal fun MedicalRecord.toLocal(): LocalMedicalRecord = when (this) {
    is WeightRecord -> LocalWeightRecord(id, petId, date.toString(), weightKg, note)
    is VaccinationRecord -> LocalVaccinationRecord(
        id = id,
        petId = petId,
        date = date.toString(),
        vaccine = vaccine,
        nextVaccinationDate = nextVaccinationDate?.toString(),
        clinic = clinic,
        veterinarian = veterinarian,
        note = note,
    )
    is VetVisitRecord -> LocalVetVisitRecord(
        id = id,
        petId = petId,
        date = date.toString(),
        reason = reason,
        clinic = clinic,
        veterinarian = veterinarian,
        diagnosis = diagnosis,
        recommendations = recommendations,
        appointmentDate = appointmentDate?.toString(),
        note = note,
    )
}