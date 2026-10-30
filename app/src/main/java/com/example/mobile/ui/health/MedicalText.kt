package com.example.mobile.ui.health

import android.content.Context
import com.example.mobile.R
import com.example.mobile.domain.model.MedicalRecord
import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.presenter.health.HealthEventType
import com.example.mobile.presenter.health.MedicalInputError
import com.example.mobile.presenter.health.PetHealthSummary
import com.example.mobile.presenter.health.UpcomingHealthEvent
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun MedicalInputError.stringResource() = when (this) {
    MedicalInputError.PET_REQUIRED -> R.string.medical_error_pet
    MedicalInputError.DATE_REQUIRED -> R.string.medical_error_date_required
    MedicalInputError.DATE_INVALID -> R.string.medical_error_date_invalid
    MedicalInputError.DATE_FUTURE -> R.string.medical_error_date_future
    MedicalInputError.WEIGHT_INVALID -> R.string.medical_error_weight
    MedicalInputError.VACCINE_REQUIRED -> R.string.medical_error_vaccine
    MedicalInputError.NEXT_DATE_ORDER -> R.string.medical_error_next_date
    MedicalInputError.REASON_REQUIRED -> R.string.medical_error_reason
}

internal fun RepositoryError.medicalStringResource() = when (this) {
    RepositoryError.READ_FAILED -> R.string.medical_error_read
    RepositoryError.WRITE_FAILED -> R.string.medical_error_write
    RepositoryError.CORRUPT_DATA -> R.string.error_local_data_corrupt
    RepositoryError.UNSUPPORTED_SCHEMA -> R.string.error_local_schema
    RepositoryError.INVALID_REFERENCE -> R.string.medical_error_pet_missing
    RepositoryError.IDENTITY_CONFLICT, RepositoryError.INVALID_DATA -> R.string.medical_error_data
}

internal class MedicalText(private val context: Context) {
    fun eventTitle(event: UpcomingHealthEvent): String = context.getString(when (event.type) {
        HealthEventType.VACCINATION -> R.string.vaccination
        HealthEventType.VET_VISIT -> R.string.vet_record
    })

    fun eventDescription(event: UpcomingHealthEvent): String = context.getString(
        R.string.pet_detail_join, inputDate(event.date), event.description,
    )

    fun petSummary(summary: PetHealthSummary): String {
        val parts = mutableListOf(context.resources.getQuantityString(R.plurals.medical_record_count, summary.recordCount, summary.recordCount))
        summary.lastWeightKg?.let { parts += weight(it) }
        summary.nextEvent?.let { parts += context.getString(R.string.pet_detail_join, eventTitle(it), inputDate(it.date)) }
        return parts.joinToString(context.getString(R.string.health_summary_separator))
    }

    fun weight(kg: Double): String = context.getString(
        R.string.medical_weight_value,
        NumberFormat.getNumberInstance(context.resources.configuration.locales[0]).apply { maximumFractionDigits = 3 }.format(kg),
        context.getString(R.string.kg),
    )

    fun title(record: MedicalRecord): String = when (record) {
        is WeightRecord -> context.getString(R.string.weight_record)
        is VaccinationRecord -> record.vaccine
        is VetVisitRecord -> record.reason
    }

    fun description(record: MedicalRecord): String {
        val parts = mutableListOf(inputDate(record.date))
        when (record) {
            is WeightRecord -> parts += weight(record.weightKg)
            is VaccinationRecord -> {
                record.nextVaccinationDate?.let { parts += context.getString(R.string.medical_next_vaccine_value, inputDate(it)) }
                record.clinic?.let(parts::add)
                record.veterinarian?.let(parts::add)
            }
            is VetVisitRecord -> {
                if (record.appointmentDate != null) parts += context.getString(R.string.medical_planned_visit)
                record.clinic?.let(parts::add)
                record.veterinarian?.let(parts::add)
                record.diagnosis?.let { parts += context.getString(R.string.pet_detail_line, context.getString(R.string.diagnosis), it) }
                record.recommendations?.let { parts += context.getString(R.string.pet_detail_line, context.getString(R.string.medical_recommendations), it) }
            }
        }
        record.note?.let(parts::add)
        return parts.joinToString("\n")
    }

    companion object {
        fun inputDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu"))
    }
}