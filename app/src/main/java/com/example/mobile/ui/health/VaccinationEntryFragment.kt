package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.health.MedicalField
import com.example.mobile.presenter.health.VaccinationEntryPresenter
import com.example.mobile.ui.common.retainedPresenter

class VaccinationEntryFragment : MedicalEntryFragment(R.layout.fragment_vaccination_entry, 1) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope -> VaccinationEntryPresenter(repositories.owner, repositories.pets, repositories.medicalRecords, petId, scope) }
        val vaccine = MedicalTextInput(view, R.id.vaccine, R.id.vaccine_field) {
            presenter.changeInput(presenter.state.value.input.copy(vaccine = it), MedicalField.VACCINE)
        }
        val date = MedicalTextInput(view, R.id.vaccine_date, R.id.vaccine_date_field) {
            presenter.changeInput(presenter.state.value.input.copy(date = it), MedicalField.DATE)
        }
        val next = MedicalTextInput(view, R.id.next_vaccine, R.id.next_vaccine_field) {
            presenter.changeInput(presenter.state.value.input.copy(nextDate = it), MedicalField.NEXT_DATE)
        }
        val clinic = MedicalTextInput(view, R.id.clinic) { presenter.changeInput(presenter.state.value.input.copy(clinic = it)) }
        val vet = MedicalTextInput(view, R.id.veterinarian) { presenter.changeInput(presenter.state.value.input.copy(veterinarian = it)) }
        val note = MedicalTextInput(view, R.id.comment) { presenter.changeInput(presenter.state.value.input.copy(note = it)) }
        date.calendar { presenter.calendarDate(MedicalField.DATE) }
        next.calendar { presenter.calendarDate(MedicalField.NEXT_DATE) }
        bindEntry(view, presenter, { field ->
            when (field) {
                MedicalField.VACCINE -> vaccine.focus()
                MedicalField.NEXT_DATE -> next.focus()
                else -> date.focus()
            }
        }) { state ->
            vaccine.render(state.input.vaccine, state.editable, state.errors[MedicalField.VACCINE])
            date.render(state.input.date, state.editable, state.errors[MedicalField.DATE])
            next.render(state.input.nextDate, state.editable, state.errors[MedicalField.NEXT_DATE])
            clinic.render(state.input.clinic, state.editable)
            vet.render(state.input.veterinarian, state.editable)
            note.render(state.input.note, state.editable)
        }
    }
}