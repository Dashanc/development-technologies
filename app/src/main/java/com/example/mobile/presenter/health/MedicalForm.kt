package com.example.mobile.presenter.health

import com.example.mobile.domain.model.MedicalRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

enum class MedicalField { PET, DATE, WEIGHT, VACCINE, NEXT_DATE, REASON }

enum class MedicalInputError { PET_REQUIRED, DATE_REQUIRED, DATE_INVALID, DATE_FUTURE, WEIGHT_INVALID, VACCINE_REQUIRED, NEXT_DATE_ORDER, REASON_REQUIRED }

data class WeightInput(val date: String = "", val weight: String = "", val note: String = "")

data class VaccinationInput(
    val vaccine: String = "", val date: String = "", val nextDate: String = "",
    val clinic: String = "", val veterinarian: String = "", val note: String = "",
)

data class VetVisitInput(
    val date: String = "", val reason: String = "", val clinic: String = "",
    val veterinarian: String = "", val diagnosis: String = "", val recommendations: String = "", val note: String = "",
)

data class PreparedMedicalRecord(val record: MedicalRecord?, val errors: Map<MedicalField, MedicalInputError>)

internal object MedicalValidation {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val weightPattern = Regex("^(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)$")

    fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value.trim(), dateFormat)
    } catch (_: DateTimeParseException) {
        null
    }

    fun date(
        value: String,
        errors: MutableMap<MedicalField, MedicalInputError>,
        field: MedicalField = MedicalField.DATE,
        optional: Boolean = false,
        allowFuture: Boolean = false,
    ): LocalDate? {
        if (value.isBlank() && optional) return null
        val date = parseDate(value)
        val error = when {
            value.isBlank() -> MedicalInputError.DATE_REQUIRED
            date == null -> MedicalInputError.DATE_INVALID
            !allowFuture && date.isAfter(LocalDate.now()) -> MedicalInputError.DATE_FUTURE
            else -> null
        }
        if (error != null) errors[field] = error
        return date
    }

    fun weight(value: String): Double? {
        val text = value.trim()
        if (!weightPattern.matches(text)) return null
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    }
}

internal fun String.optionalText() = trim().takeIf(String::isNotEmpty)