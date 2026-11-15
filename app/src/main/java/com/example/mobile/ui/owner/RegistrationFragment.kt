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
        val presenter = retainedPresenter { scope ->
            RegistrationPresenter(
                repositories.authentication,
                repositories.localOwner,
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
        val openProfile = view.findViewById<MaterialButton>(R.id.sign_in)
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
            if (presenter.submit(password.text.toString().toCharArray())) password.text.clear()
            val state = presenter.state.value
            if (!fields.focusError(state.errors)) {
                if (state.passwordInvalid) password.requestFocus() else if (state.consentRequired) consent.requestFocus()
            }
        }
        openProfile.setOnClickListener { password.text.clear(); presenter.toggleLogin() }
        view.findViewById<View>(R.id.open_local).setOnClickListener { password.text.clear(); presenter.openLocalProfile() }
        view.findViewById<View>(R.id.forget_session).setOnClickListener { presenter.forgetSession() }
        renderState(presenter.state) { state ->
            val enabled = !state.loading && !state.saving && !state.loadFailed && !state.hasOwner
            fields.render(state.input, state.errors, enabled, state.serverErrors)
            listOf(R.id.name_label, R.id.owner_name_field, R.id.phone_label, R.id.owner_phone_field).forEach {
                view.findViewById<View>(it).isVisible = !state.login
            }
            view.findViewById<TextView>(R.id.auth_title).setText(if (state.login) R.string.auth_login else R.string.create_account)
            password.isEnabled = enabled
            passwordField.error = if (state.passwordInvalid) getString(if (state.login) R.string.auth_password_required else R.string.auth_password_length) else state.serverErrors["password"]
            consent.isVisible = !state.login
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
                state.hasOwner -> R.string.auth_open_profile
                state.login -> R.string.auth_login
                else -> R.string.create_account
            })
            openProfile.isEnabled = !state.loading && !state.saving && !state.loadFailed
            openProfile.setText(if (state.login) R.string.auth_create_instead else R.string.have_account)
            view.findViewById<View>(R.id.open_local).apply { isVisible = state.hasLocalProfile && !state.hasOwner; isEnabled = !state.loading && !state.saving }
            view.findViewById<View>(R.id.forget_session).apply { isVisible = state.loadFailed; isEnabled = !state.loading && !state.saving }
            val messageId = state.repositoryError?.stringResource() ?: when {
                state.loading -> R.string.profile_loading
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