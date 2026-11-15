package com.example.mobile.presenter.health

import com.example.mobile.domain.model.VetVisitRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope

class VetVisitEntryPresenter(owners: OwnerRepository, pets: PetRepository, records: MedicalRecordRepository, petId: String?, scope: CoroutineScope) :
    MedicalEntryPresenter<VetVisitInput>(VetVisitInput(), owners, pets, records, petId, scope) {

    override fun dateText(input: VetVisitInput, field: MedicalField) = input.date

    override fun prepare(id: String, petId: String, input: VetVisitInput): PreparedMedicalRecord {
        val errors = mutableMapOf<MedicalField, MedicalInputError>()
        val date = MedicalValidation.date(input.date, errors, allowFuture = true)
        if (input.reason.isBlank()) errors[MedicalField.REASON] = MedicalInputError.REASON_REQUIRED
        val record = if (errors.isEmpty()) VetVisitRecord(
            id = id, petId = petId, date = requireNotNull(date), reason = input.reason.trim(),
            clinic = input.clinic.optionalText(), veterinarian = input.veterinarian.optionalText(),
            diagnosis = input.diagnosis.optionalText(), recommendations = input.recommendations.optionalText(),
            appointmentDate = date.takeIf { it.isAfter(LocalDate.now()) }, note = input.note.optionalText(),
        ) else null
        return PreparedMedicalRecord(record, errors)
    }
}