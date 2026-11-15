package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.health.MedicalField
import com.example.mobile.presenter.health.VetVisitEntryPresenter
import com.example.mobile.ui.common.retainedPresenter

class VetVisitEntryFragment : MedicalEntryFragment(R.layout.fragment_vet_visit_entry, 2) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope -> VetVisitEntryPresenter(repositories.owner, repositories.pets, repositories.medicalRecords, petId, scope) }
        val date = MedicalTextInput(view, R.id.visit_date, R.id.visit_date_field) {
            presenter.changeInput(presenter.state.value.input.copy(date = it), MedicalField.DATE)
        }
        val reason = MedicalTextInput(view, R.id.reason, R.id.reason_field) {
            presenter.changeInput(presenter.state.value.input.copy(reason = it), MedicalField.REASON)
        }
        val clinic = MedicalTextInput(view, R.id.clinic) { presenter.changeInput(presenter.state.value.input.copy(clinic = it)) }
        val vet = MedicalTextInput(view, R.id.veterinarian) { presenter.changeInput(presenter.state.value.input.copy(veterinarian = it)) }
        val diagnosis = MedicalTextInput(view, R.id.diagnosis) { presenter.changeInput(presenter.state.value.input.copy(diagnosis = it)) }
        val recommendations = MedicalTextInput(view, R.id.recommendations) { presenter.changeInput(presenter.state.value.input.copy(recommendations = it)) }
        val note = MedicalTextInput(view, R.id.comment) { presenter.changeInput(presenter.state.value.input.copy(note = it)) }
        date.calendar { presenter.calendarDate(MedicalField.DATE) }
        bindEntry(view, presenter, { if (it == MedicalField.REASON) reason.focus() else date.focus() }) { state ->
            date.render(state.input.date, state.editable, state.errors[MedicalField.DATE])
            reason.render(state.input.reason, state.editable, state.errors[MedicalField.REASON])
            clinic.render(state.input.clinic, state.editable)
            vet.render(state.input.veterinarian, state.editable)
            diagnosis.render(state.input.diagnosis, state.editable)
            recommendations.render(state.input.recommendations, state.editable)
            note.render(state.input.note, state.editable)
        }
    }
}