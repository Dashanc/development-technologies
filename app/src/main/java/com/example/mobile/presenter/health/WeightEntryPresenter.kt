package com.example.mobile.presenter.health

import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import kotlinx.coroutines.CoroutineScope

class WeightEntryPresenter(owners: OwnerRepository, pets: PetRepository, records: MedicalRecordRepository, petId: String?, scope: CoroutineScope) :
    MedicalEntryPresenter<WeightInput>(WeightInput(), owners, pets, records, petId, scope) {

    override fun dateText(input: WeightInput, field: MedicalField) = input.date

    override fun prepare(id: String, petId: String, input: WeightInput): PreparedMedicalRecord {
        val errors = mutableMapOf<MedicalField, MedicalInputError>()
        val date = MedicalValidation.date(input.date, errors)
        val weight = MedicalValidation.weight(input.weight)
        if (weight == null) errors[MedicalField.WEIGHT] = MedicalInputError.WEIGHT_INVALID
        val record = if (errors.isEmpty()) WeightRecord(id, petId, requireNotNull(date), requireNotNull(weight), input.note.optionalText()) else null
        return PreparedMedicalRecord(record, errors)
    }
}