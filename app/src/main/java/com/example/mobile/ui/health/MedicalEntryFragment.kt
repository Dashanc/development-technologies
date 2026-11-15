package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.example.mobile.R
import com.example.mobile.presenter.health.MedicalDestination
import com.example.mobile.presenter.health.MedicalEntryPresenter
import com.example.mobile.presenter.health.MedicalEntryState
import com.example.mobile.presenter.health.MedicalField
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.whenResumed
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton

abstract class MedicalEntryFragment(
    @LayoutRes layout: Int,
    private val typeIndex: Int,
) : ScreenFragment(layout) {
    private var recordType: Spinner? = null
    private var selectedPet: () -> String? = { petId }
    private var canSwitchType: () -> Boolean = { true }

    protected fun <I> bindEntry(
        view: View,
        presenter: MedicalEntryPresenter<I>,
        focusError: (MedicalField) -> Unit,
        renderFields: (MedicalEntryState<I>) -> Unit,
    ) {
        selectedPet = { presenter.state.value.selectedPetId }
        canSwitchType = { !presenter.state.value.saving }
        val selector = view.findViewById<Spinner>(R.id.pet_selector)
        val petError = view.findViewById<TextView>(R.id.medical_pet_error)
        val message = view.findViewById<TextView>(R.id.medical_message)
        val save = view.findViewById<MaterialButton>(R.id.save)
        var choices = emptyList<Pair<String, String>>()
        var rendering = false
        selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                if (!rendering && position == selector.selectedItemPosition && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    presenter.selectPet(choices.getOrNull(position - 1)?.first)
                }
            }
        }
        save.setOnClickListener {
            presenter.submit()
            presenter.state.value.errors.keys.firstOrNull()?.let { field ->
                if (field == MedicalField.PET) selector.requestFocus() else focusError(field)
            }
        }
        renderState(presenter.state) { state ->
            rendering = true
            val nextChoices = state.pets.map { it.id to it.name }
            if (selector.adapter == null || choices != nextChoices) {
                choices = nextChoices
                selector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
                    listOf(getString(R.string.medical_choose_pet)) + choices.map { it.second },
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            selector.setSelection(choices.indexOfFirst { it.first == state.selectedPetId } + 1)
            selector.isEnabled = state.editable
            petError.isVisible = state.errors.containsKey(MedicalField.PET)
            recordType?.isEnabled = !state.loading && !state.saving
            save.isEnabled = !state.loading && !state.saving
            save.setText(when {
                state.loading -> R.string.medical_loading
                state.saving -> R.string.profile_saving
                state.loadFailed -> R.string.retry
                state.ownerMissing -> R.string.create_account
                state.pets.isEmpty() -> R.string.add_pet
                typeIndex == 1 -> R.string.save_vaccine
                typeIndex == 2 -> R.string.save_visit
                else -> R.string.save_record
            })
            view.findViewById<View>(R.id.back).isEnabled = !state.saving
            val messageId = state.error?.medicalStringResource() ?: when {
                state.ownerMissing -> R.string.local_profile_missing
                !state.loading && state.pets.isEmpty() -> R.string.medical_no_pets
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            renderFields(state)
            rendering = false
            if (state.savedPetId != null || state.destination != null) whenResumed(view) {
                val saved = presenter.takeSavedPet()
                if (saved != null) {
                    Toast.makeText(requireContext(), R.string.medical_saved, Toast.LENGTH_SHORT).show()
                    if (petId != null && petId != saved) navigator.replace(Screen.PET_PROFILE, saved) else navigator.back()
                } else when (presenter.takeDestination()) {
                    MedicalDestination.REGISTRATION -> navigator.openRoot(Screen.REGISTRATION)
                    MedicalDestination.ADD_PET -> navigator.open(Screen.ADD_PET)
                    null -> Unit
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        recordType = requireView().findViewById<Spinner>(R.id.record_type).apply {
            setSelection(typeIndex, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == typeIndex || !canSwitchType() || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
                    val screen = when (position) {
                        1 -> Screen.VACCINATION_ENTRY
                        2 -> Screen.VET_VISIT_ENTRY
                        else -> Screen.WEIGHT_ENTRY
                    }
                    navigator.replace(screen, selectedPet())
                }
            }
        }
    }

    override fun onDestroyView() {
        recordType?.onItemSelectedListener = null
        recordType = null
        selectedPet = { petId }
        canSwitchType = { true }
        super.onDestroyView()
    }
}