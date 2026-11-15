package com.example.mobile.ui.health

import android.app.DatePickerDialog
import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.core.widget.doAfterTextChanged
import com.example.mobile.presenter.health.MedicalInputError
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

internal class MedicalTextInput(
    view: View,
    @IdRes inputId: Int,
    @IdRes layoutId: Int = 0,
    changed: (String) -> Unit,
) {
    private val input = view.findViewById<EditText>(inputId)
    private val layout = if (layoutId == 0) null else view.findViewById<TextInputLayout>(layoutId)
    private var rendering = false

    init {
        input.doAfterTextChanged { if (!rendering) changed(it.toString()) }
    }

    fun render(value: String, enabled: Boolean, error: MedicalInputError? = null) {
        rendering = true
        if (input.text.toString() != value) {
            input.setText(value)
            input.setSelection(input.text.length)
        }
        input.isEnabled = enabled
        layout?.error = error?.let { input.context.getString(it.stringResource()) }
        layout?.let { it.isEndIconVisible = enabled && it.endIconMode == TextInputLayout.END_ICON_CUSTOM }
        rendering = false
    }

    fun focus() = input.requestFocus()

    fun calendar(initial: () -> LocalDate) {
        layout?.setEndIconOnClickListener {
            val date = initial()
            DatePickerDialog(input.context, { _, year, month, day ->
                input.setText(MedicalText.inputDate(LocalDate.of(year, month + 1, day)))
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }
    }
}