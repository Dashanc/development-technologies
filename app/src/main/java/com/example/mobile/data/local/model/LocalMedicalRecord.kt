package com.example.mobile.data.local.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface LocalMedicalRecord {
    val id: String
    val petId: String
    val date: String
    val note: String?
}

@Serializable
@SerialName("weight")
internal data class LocalWeightRecord(
    override val id: String,
    override val petId: String,
    override val date: String,
    val weightKg: Double,
    override val note: String? = null,
) : LocalMedicalRecord

@Serializable
@SerialName("vaccination")
internal data class LocalVaccinationRecord(
    override val id: String,
    override val petId: String,
    override val date: String,
    val vaccine: String,
    val nextVaccinationDate: String? = null,
    val clinic: String? = null,
    val veterinarian: String? = null,
    override val note: String? = null,
) : LocalMedicalRecord

@Serializable
@SerialName("vet_visit")
internal data class LocalVetVisitRecord(
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
) : LocalMedicalRecord