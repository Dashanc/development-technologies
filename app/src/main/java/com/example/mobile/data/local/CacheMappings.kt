package com.example.mobile.data.local

import com.example.mobile.data.local.model.CachedOwner
import com.example.mobile.data.local.model.CachedPet
import com.example.mobile.data.local.model.CachedRecord
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.model.Pet
import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord

internal fun Owner.toCache() = CachedOwner(toLocal(), revision)
internal fun Pet.toCache() = CachedPet(copy(photoUri = null).toLocal(), revision)
internal fun MedicalRecord.toCache() = CachedRecord(toLocal(), revision)
internal fun CachedOwner.toDomain() = value.toDomain().copy(revision = revision)
internal fun CachedPet.toDomain(photo: String?) = value.toDomain().copy(revision = revision, photoUri = photo)
internal fun CachedRecord.toDomain(): MedicalRecord = when (val record = value.toDomain()) {
    is WeightRecord -> record.copy(revision = revision)
    is VaccinationRecord -> record.copy(revision = revision)
    is VetVisitRecord -> record.copy(revision = revision)
}