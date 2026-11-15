package com.example.mobile.presenter.health

import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import java.time.LocalDate

enum class HealthEventType { VACCINATION, VET_VISIT }

data class UpcomingHealthEvent(
    val recordId: String,
    val petId: String,
    val petName: String,
    val date: LocalDate,
    val type: HealthEventType,
    val description: String,
)

internal fun List<MedicalHistoryItem>.upcomingEvents(today: LocalDate): List<UpcomingHealthEvent> = mapNotNull { item ->
    val record = item.record
    val event = when (record) {
        is WeightRecord -> null
        is VaccinationRecord -> record.nextVaccinationDate?.let {
            UpcomingHealthEvent(record.id, record.petId, item.petName, it, HealthEventType.VACCINATION, record.vaccine)
        }
        is VetVisitRecord -> record.appointmentDate?.let {
            UpcomingHealthEvent(record.id, record.petId, item.petName, it, HealthEventType.VET_VISIT, record.reason)
        }
    }
    event?.takeUnless { it.date.isBefore(today) }
}.sortedWith(compareBy<UpcomingHealthEvent> { it.date }.thenBy { it.petName }.thenBy { it.recordId })