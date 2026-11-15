package com.example.mobile.ui.owner

import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.example.mobile.R
import com.example.mobile.presenter.owner.OwnerFieldError
import com.example.mobile.presenter.owner.OwnerInput
import com.example.mobile.presenter.owner.OwnerInputErrors
import com.example.mobile.ui.common.PhoneMask
import com.google.android.material.textfield.TextInputLayout

internal class OwnerFormFields(view: View, private val changed: (OwnerInput) -> Unit) {
    private val name = view.findViewById<EditText>(R.id.owner_name)
    private val email = view.findViewById<EditText>(R.id.owner_email)
    private val phone = view.findViewById<EditText>(R.id.owner_phone)
    private val nameField = view.findViewById<TextInputLayout>(R.id.owner_name_field)
    private val emailField = view.findViewById<TextInputLayout>(R.id.owner_email_field)
    private val phoneField = view.findViewById<TextInputLayout>(R.id.owner_phone_field)
    private var rendering = false
    private val phoneMask = PhoneMask(view.context.getString(R.string.phone_hint))

    init {
        listOf(name, email, phone).forEach { field ->
            field.doAfterTextChanged {
                if (!rendering) changed(OwnerInput(name.text.toString(), email.text.toString(), phone.text.toString()))
            }
        }
    }

    fun render(input: OwnerInput, errors: OwnerInputErrors, enabled: Boolean) {
        rendering = true
        name.setValue(input.name)
        email.setValue(input.email)
        phone.setValue(input.phone)
        listOf(name, email, phone).forEach { it.isEnabled = enabled }
        nameField.error = errors.name?.let { name.context.getString(it.stringResource()) }
        emailField.error = errors.email?.let { email.context.getString(it.stringResource()) }
        phoneField.error = errors.phone?.let { phone.context.getString(it.stringResource()) }
        rendering = false
    }

    fun focusError(errors: OwnerInputErrors): Boolean {
        val field = when {
            errors.name != null -> name
            errors.email != null -> email
            errors.phone != null -> phone
            else -> return false
        }
        field.requestFocus()
        return true
    }

    private fun EditText.setValue(value: String) {
        val display = if (this === phone) phoneMask.format(value) else value
        if (text.toString() != display) {
            setText(display)
            setSelection(text.length)
        }
    }

    private fun OwnerFieldError.stringResource() = when (this) {
        OwnerFieldError.NAME_REQUIRED -> R.string.error_name_required
        OwnerFieldError.EMAIL_REQUIRED -> R.string.error_email_required
        OwnerFieldError.EMAIL_INVALID -> R.string.error_email_invalid
        OwnerFieldError.PHONE_INVALID -> R.string.error_phone_invalid
    }
}