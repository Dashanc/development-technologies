package com.example.mobile.domain.model

import java.time.LocalDate

sealed interface MedicalRecord {
    val id: String
    val petId: String
    val date: LocalDate
    val note: String?
}

data class WeightRecord(
    override val id: String,
    override val petId: String,
    override val date: LocalDate,
    val weightKg: Double,
    override val note: String? = null,
) : MedicalRecord

data class VaccinationRecord(
    override val id: String,
    override val petId: String,
    override val date: LocalDate,
    val vaccine: String,
    val nextVaccinationDate: LocalDate? = null,
    val clinic: String? = null,
    val veterinarian: String? = null,
    override val note: String? = null,
) : MedicalRecord

data class VetVisitRecord(
    override val id: String,
    override val petId: String,
    override val date: LocalDate,
    val reason: String,
    val clinic: String? = null,
    val veterinarian: String? = null,
    val diagnosis: String? = null,
    val recommendations: String? = null,
    val appointmentDate: LocalDate? = null,
    override val note: String? = null,
) : MedicalRecord