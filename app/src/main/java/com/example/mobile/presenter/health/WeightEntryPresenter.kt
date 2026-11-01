package com.example.mobile.presenter.health

import com.example.mobile.domain.model.WeightRecord
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.SettingsRepository
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope

class WeightEntryPresenter(owners: OwnerRepository, pets: PetRepository, records: MedicalRecordRepository, petId: String?, scope: CoroutineScope, settings: SettingsRepository) :
    MedicalEntryPresenter<WeightInput>(WeightInput(), owners, pets, records, petId, scope, settings) {

    override fun dateText(input: WeightInput, field: MedicalField) = input.date

    override fun prepare(id: String, petId: String, input: WeightInput): PreparedMedicalRecord {
        val errors = mutableMapOf<MedicalField, MedicalInputError>()
        val date = MedicalValidation.date(input.date, errors)
        val weight = MedicalValidation.weight(input.weight)?.let(state.value.weightUnit::toKilograms)?.takeIf { it.isFinite() && it > 0 }
        if (weight == null) errors[MedicalField.WEIGHT] = MedicalInputError.WEIGHT_INVALID
        val record = if (errors.isEmpty()) WeightRecord(id, petId, requireNotNull(date), requireNotNull(weight), input.note.optionalText()) else null
        return PreparedMedicalRecord(record, errors)
    }

    override fun unitChanged(input: WeightInput, from: WeightUnit, to: WeightUnit): WeightInput {
        if (from == to) return input
        val weight = MedicalValidation.weight(input.weight) ?: return input
        val converted = to.fromKilograms(from.toKilograms(weight))
        return if (converted.isFinite()) input.copy(weight = BigDecimal.valueOf(converted).stripTrailingZeros().toPlainString()) else input
    }
}