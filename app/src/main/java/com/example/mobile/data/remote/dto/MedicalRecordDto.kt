package com.example.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface MedicalRecordDto {
    val id: String
    val petId: String
    val date: String
    val note: String?
}

@Serializable
@SerialName("WEIGHT")
internal data class WeightDto(
    override val id: String,
    override val petId: String,
    override val date: String,
    val weightKg: Double,
    override val note: String? = null,
) : MedicalRecordDto

@Serializable
@SerialName("VACCINATION")
internal data class VaccinationDto(
    override val id: String,
    override val petId: String,
    override val date: String,
    val vaccine: String,
    val nextVaccinationDate: String? = null,
    val clinic: String? = null,
    val veterinarian: String? = null,
    override val note: String? = null,
) : MedicalRecordDto

@Serializable
@SerialName("VET_VISIT")
internal data class VetVisitDto(
    override val id: String,
    override val petId: String,
    override val date: String,
    val reason: String,
    val clinic: String? = null,
    val veterinarian: String? = null,
    val diagnosis: String? = null,
    val recommendations: String? = null,
    val appointmentDate: String? = null,
    override val note: String? = null,
) : MedicalRecordDto

@Serializable
internal data class WeightWriteDto(val date: String, val weightKg: Double, val note: String?)

@Serializable
internal data class VaccinationWriteDto(
    val date: String, val vaccine: String, val nextVaccinationDate: String?,
    val clinic: String?, val veterinarian: String?, val note: String?,
)

@Serializable
internal data class VetVisitWriteDto(
    val date: String, val reason: String, val clinic: String?, val veterinarian: String?,
    val diagnosis: String?, val recommendations: String?, val appointmentDate: String?, val note: String?,
)