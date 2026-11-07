package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.owner.RegistrationMessage
import com.example.mobile.presenter.owner.RegistrationPresenter
import com.example.mobile.ui.common.InformationDialog
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.whenResumed
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class RegistrationFragment : ScreenFragment(R.layout.fragment_registration) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope ->
            RegistrationPresenter(
                repositories.owner,
                repositories.settings,
                scope,
                arguments?.getBoolean(Screen.ARG_RESUME_PROFILE) == true,
            )
        }
        val fields = OwnerFormFields(view, presenter::changeInput)
        val password = view.findViewById<EditText>(R.id.password)
        val passwordField = view.findViewById<TextInputLayout>(R.id.password_field)
        val consent = view.findViewById<CheckBox>(R.id.consent)
        val consentError = view.findViewById<TextView>(R.id.consent_error)
        val language = view.findViewById<Spinner>(R.id.language)
        val register = view.findViewById<MaterialButton>(R.id.register)
        val openProfile = view.findViewById<View>(R.id.sign_in)
        val message = view.findViewById<TextView>(R.id.form_message)
        password.doAfterTextChanged { presenter.passwordChanged() }
        consent.setOnCheckedChangeListener { _, checked -> presenter.changeConsent(checked) }
        language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                presenter.changeLanguage(RegistrationLanguage.options[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        register.setOnClickListener {
            presenter.submit(password.text)
            val state = presenter.state.value
            if (!fields.focusError(state.errors)) {
                if (state.passwordTooShort) password.requestFocus() else if (state.consentRequired) consent.requestFocus()
            }
        }
        openProfile.setOnClickListener { presenter.openExistingProfile() }
        renderState(presenter.state) { state ->
            val enabled = !state.loading && !state.saving && !state.loadFailed && !state.hasOwner
            fields.render(state.input, state.errors, enabled)
            password.isEnabled = enabled
            passwordField.error = if (state.passwordTooShort) getString(R.string.error_password_short) else null
            consent.isEnabled = enabled
            consent.isChecked = state.consent
            consentError.isVisible = state.consentRequired
            language.isEnabled = enabled
            language.setSelection(RegistrationLanguage.options.indexOf(state.language))
            register.isEnabled = !state.loading && !state.saving
            register.setText(when {
                state.loading -> R.string.profile_loading
                state.saving -> R.string.profile_saving
                state.loadFailed -> R.string.retry
                state.hasOwner -> R.string.open_local_profile
                else -> R.string.create_account
            })
            openProfile.isEnabled = !state.loading && !state.saving && !state.loadFailed
            val messageId = state.repositoryError?.stringResource() ?: when {
                state.loading -> R.string.profile_loading
                state.message == RegistrationMessage.EXISTING_PROFILE -> R.string.local_profile_exists
                state.message == RegistrationMessage.NO_PROFILE -> R.string.local_profile_missing
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            if (state.openProfile) whenResumed(view) {
                password.text.clear()
                if (presenter.takeNavigation()) {
                    navigator.openRoot(Screen.OWNER_PROFILE)
                    parentFragmentManager.executePendingTransactions()
                    RegistrationLanguage.apply(state.language)
                }
            }
        }
        view.findViewById<View>(R.id.terms).setOnClickListener {
            InformationDialog.show(this, R.string.terms, R.string.terms_unavailable)
        }
        view.findViewById<View>(R.id.privacy).setOnClickListener {
            InformationDialog.show(this, R.string.privacy, R.string.privacy_unavailable)
        }
    }
}