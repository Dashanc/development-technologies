package com.example.mobile.data.remote

import com.example.mobile.data.remote.dto.MedicalRecordDto
import com.example.mobile.data.remote.dto.OwnerDto
import com.example.mobile.data.remote.dto.PetDto
import com.example.mobile.data.remote.dto.PetExtraDto
import com.example.mobile.data.remote.dto.PetWriteDto
import com.example.mobile.data.remote.dto.VaccinationDto
import com.example.mobile.data.remote.dto.VaccinationWriteDto
import com.example.mobile.data.remote.dto.VetVisitDto
import com.example.mobile.data.remote.dto.VetVisitWriteDto
import com.example.mobile.data.remote.dto.WeightDto
import com.example.mobile.data.remote.dto.WeightWriteDto
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.domain.model.PetSex
import com.example.mobile.domain.model.PetSpecies
import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.repository.RepositoryError
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal fun String.canonicalId(): String = UUID.fromString(this).toString().also {
    require(it.equals(this, ignoreCase = true))
}

internal fun validateId(id: String) {
    try {
        id.canonicalId()
    } catch (_: IllegalArgumentException) {
        throw RemoteFailure(RepositoryError.INVALID_DATA)
    }
}

internal fun OwnerDto.toDomain(revision: String?) = Owner(id.canonicalId(), name, email, phone.orEmpty(), revision)

internal fun PetDto.toDomain(photoUri: String?, revision: String?) = Pet(
    id = id.canonicalId(), ownerId = ownerId.canonicalId(), name = name,
    species = PetSpecies.valueOf(species), breed = breed, birthDate = birthDate?.let(LocalDate::parse),
    sex = PetSex.valueOf(sex), photoUri = photoUri,
    extra = PetExtraData(extra.coat, extra.microchip, extra.passportNumber, extra.neutered, extra.allergies),
    revision = revision,
)

internal fun Pet.toWriteDto() = PetWriteDto(
    name, species.name, breed, birthDate?.toString(), sex.name,
    PetExtraDto(extra.coat, extra.microchip, extra.passportNumber, extra.neutered, extra.allergies),
)

internal fun MedicalRecordDto.location(): RecordLocation = RecordLocation(petId.canonicalId(), when (this) {
    is WeightDto -> "weight-records"
    is VaccinationDto -> "vaccination-records"
    is VetVisitDto -> "vet-visits"
})

internal fun MedicalRecord.location(): RecordLocation = RecordLocation(petId, when (this) {
    is WeightRecord -> "weight-records"
    is VaccinationRecord -> "vaccination-records"
    is VetVisitRecord -> "vet-visits"
})

internal fun MedicalRecordDto.toDomain(revision: String?): MedicalRecord {
    val id = id.canonicalId()
    val pet = petId.canonicalId()
    val date = LocalDate.parse(date)
    return when (this) {
        is WeightDto -> {
            require(weightKg.isFinite() && weightKg > 0)
            WeightRecord(id, pet, date, weightKg, note, revision)
        }
        is VaccinationDto -> VaccinationRecord(
            id, pet, date, vaccine, nextVaccinationDate?.let(LocalDate::parse), clinic, veterinarian, note, revision,
        )
        is VetVisitDto -> VetVisitRecord(
            id, pet, date, reason, clinic, veterinarian, diagnosis, recommendations,
            appointmentDate?.let(LocalDate::parse), note, revision,
        )
    }
}

internal fun MedicalRecord.toWriteJson(json: Json): JsonElement = when (this) {
    is WeightRecord -> {
        if (!weightKg.isFinite() || weightKg <= 0) throw RemoteFailure(RepositoryError.INVALID_DATA)
        json.encodeToJsonElement(WeightWriteDto(date.toString(), weightKg, note))
    }
    is VaccinationRecord -> json.encodeToJsonElement(VaccinationWriteDto(
        date.toString(), vaccine, nextVaccinationDate?.toString(), clinic, veterinarian, note,
    ))
    is VetVisitRecord -> json.encodeToJsonElement(VetVisitWriteDto(
        date.toString(), reason, clinic, veterinarian, diagnosis, recommendations, appointmentDate?.toString(), note,
    ))
}