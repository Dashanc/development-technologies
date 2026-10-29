package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.health.MedicalField
import com.example.mobile.presenter.health.WeightEntryPresenter
import com.example.mobile.ui.common.retainedPresenter

class WeightEntryFragment : MedicalEntryFragment(R.layout.fragment_weight_entry, 0) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope -> WeightEntryPresenter(repositories.owner, repositories.pets, repositories.medicalRecords, petId, scope) }
        val date = MedicalTextInput(view, R.id.record_date, R.id.record_date_field) {
            presenter.changeInput(presenter.state.value.input.copy(date = it), MedicalField.DATE)
        }
        val weight = MedicalTextInput(view, R.id.record_weight, R.id.record_weight_field) {
            presenter.changeInput(presenter.state.value.input.copy(weight = it), MedicalField.WEIGHT)
        }
        val note = MedicalTextInput(view, R.id.record_comment) { presenter.changeInput(presenter.state.value.input.copy(note = it)) }
        date.calendar { presenter.calendarDate(MedicalField.DATE) }
        bindEntry(view, presenter, { if (it == MedicalField.DATE) date.focus() else weight.focus() }) { state ->
            date.render(state.input.date, state.editable, state.errors[MedicalField.DATE])
            weight.render(state.input.weight, state.editable, state.errors[MedicalField.WEIGHT])
            note.render(state.input.note, state.editable)
        }
    }
}