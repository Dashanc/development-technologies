package com.example.mobile.ui.pets

import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.mobile.domain.model.PetSex
import com.example.mobile.domain.model.PetSpecies
import com.example.mobile.presenter.pets.PetInput
import com.example.mobile.presenter.pets.PetInputErrors
import com.example.mobile.R
import com.google.android.material.textfield.TextInputLayout

internal class PetFormFields(
    view: View,
    private val current: () -> PetInput,
    private val changed: (PetInput) -> Unit,
) {
    private val name = view.findViewById<EditText>(R.id.pet_name)
    private val breed = view.findViewById<EditText>(R.id.breed)
    private val birth = view.findViewById<EditText>(R.id.birth)
    private val species = view.findViewById<RadioGroup>(R.id.species)
    private val sex = view.findViewById<RadioGroup>(R.id.sex)
    private val nameField = view.findViewById<TextInputLayout>(R.id.pet_name_field)
    private val birthField = view.findViewById<TextInputLayout>(R.id.pet_birth_field)
    private val sexError = view.findViewById<TextView>(R.id.pet_sex_error)
    private var rendering = false

    init {
        listOf(name, breed, birth).forEach { field -> field.doAfterTextChanged { notifyChanged() } }
        species.setOnCheckedChangeListener { _, _ -> notifyChanged() }
        sex.setOnCheckedChangeListener { _, _ -> notifyChanged() }
    }

    private fun notifyChanged() {
        if (rendering) return
        changed(current().copy(
            name = name.text.toString(), breed = breed.text.toString(), birth = birth.text.toString(),
            species = when (species.checkedRadioButtonId) {
                R.id.species_1 -> PetSpecies.DOG
                R.id.species_2 -> PetSpecies.OTHER
                else -> PetSpecies.CAT
            },
            sex = when (sex.checkedRadioButtonId) {
                R.id.sex_0 -> PetSex.MALE
                R.id.sex_1 -> PetSex.FEMALE
                else -> PetSex.UNKNOWN
            },
        ))
    }

    fun render(input: PetInput, errors: PetInputErrors, enabled: Boolean) {
        rendering = true
        name.setValue(input.name)
        breed.setValue(input.breed)
        birth.setValue(input.birth)
        species.check(when (input.species) {
            PetSpecies.CAT -> R.id.species_0
            PetSpecies.DOG -> R.id.species_1
            PetSpecies.OTHER -> R.id.species_2
        })
        sex.check(when (input.sex) {
            PetSex.MALE -> R.id.sex_0
            PetSex.FEMALE -> R.id.sex_1
            PetSex.UNKNOWN -> -1
        })
        listOf(name, breed, birth).forEach { it.isEnabled = enabled }
        listOf(species, sex).forEach { group ->
            for (index in 0 until group.childCount) group.getChildAt(index).isEnabled = enabled
        }
        nameField.error = errors.name?.let { name.context.getString(it.stringResource()) }
        birthField.error = errors.birth?.let { birth.context.getString(it.stringResource()) }
        birthField.isEndIconVisible = enabled
        sexError.isVisible = errors.sex != null
        rendering = false
    }

    fun showCalendar(action: () -> Unit) = birthField.setEndIconOnClickListener { action() }

    fun setBirth(value: String) {
        birth.setValue(value)
    }

    fun focusError(errors: PetInputErrors) {
        when {
            errors.name != null -> name.requestFocus()
            errors.birth != null -> birth.requestFocus()
            errors.sex != null -> sex.getChildAt(0).requestFocus()
        }
    }

    private fun EditText.setValue(value: String) {
        if (text.toString() != value) {
            setText(value)
            setSelection(text.length)
        }
    }
}