package com.example.mobile.presenter.health

import com.example.mobile.domain.model.VaccinationRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import kotlinx.coroutines.CoroutineScope

class VaccinationEntryPresenter(owners: OwnerRepository, pets: PetRepository, records: MedicalRecordRepository, petId: String?, scope: CoroutineScope) :
    MedicalEntryPresenter<VaccinationInput>(VaccinationInput(), owners, pets, records, petId, scope) {

    override fun dateText(input: VaccinationInput, field: MedicalField) = if (field == MedicalField.NEXT_DATE) input.nextDate else input.date

    override fun prepare(id: String, petId: String, input: VaccinationInput): PreparedMedicalRecord {
        val errors = mutableMapOf<MedicalField, MedicalInputError>()
        val date = MedicalValidation.date(input.date, errors)
        val next = MedicalValidation.date(input.nextDate, errors, MedicalField.NEXT_DATE, optional = true, allowFuture = true)
        if (input.vaccine.isBlank()) errors[MedicalField.VACCINE] = MedicalInputError.VACCINE_REQUIRED
        if (date != null && next != null && !next.isAfter(date)) errors[MedicalField.NEXT_DATE] = MedicalInputError.NEXT_DATE_ORDER
        val record = if (errors.isEmpty()) VaccinationRecord(
            id, petId, requireNotNull(date), input.vaccine.trim(), next,
            input.clinic.optionalText(), input.veterinarian.optionalText(), input.note.optionalText(),
        ) else null
        return PreparedMedicalRecord(record, errors)
    }
}