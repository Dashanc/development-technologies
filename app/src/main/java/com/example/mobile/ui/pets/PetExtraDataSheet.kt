package com.example.mobile.ui.pets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.R
import com.example.mobile.ui.common.renderState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout

class PetExtraDataSheet : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.sheet_pet_extra_data, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = requireParentFragment() as PetExtraDataHost
        val coat = view.findViewById<EditText>(R.id.coat)
        val microchip = view.findViewById<EditText>(R.id.microchip)
        val passport = view.findViewById<EditText>(R.id.passport)
        val allergies = view.findViewById<EditText>(R.id.allergies)
        val neutered = view.findViewById<CompoundButton>(R.id.neutered)
        val microchipField = view.findViewById<TextInputLayout>(R.id.microchip_field)
        var rendering = false
        fun changed() {
            if (!rendering) host.changeExtra(PetExtraData(
                coat = coat.text.toString(), microchip = microchip.text.toString(),
                passportNumber = passport.text.toString(), neutered = neutered.isChecked,
                allergies = allergies.text.toString(),
            ))
        }
        listOf(coat, microchip, passport, allergies).forEach { it.doAfterTextChanged { changed() } }
        neutered.setOnCheckedChangeListener { _, _ -> changed() }
        view.findViewById<View>(R.id.done).setOnClickListener {
            if (host.confirmExtra()) dismiss() else microchip.requestFocus()
        }
        renderState(host.extraDataState) { state ->
            rendering = true
            val values = state.extra
            listOf(coat to values.coat, microchip to values.microchip, passport to values.passportNumber, allergies to values.allergies)
                .forEach { (field, value) ->
                    if (field.text.toString() != value.orEmpty()) field.setText(value.orEmpty())
                    field.isEnabled = state.editable
                }
            neutered.isChecked = values.neutered
            neutered.isEnabled = state.editable
            microchipField.error = state.microchipError?.let { getString(it.stringResource()) }
            rendering = false
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    companion object {
        const val TAG = "pet_extra_data"
    }
}