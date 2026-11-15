package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.owner.EditProfilePresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.whenResumed
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton

class EditProfileFragment : ScreenFragment(R.layout.fragment_edit_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope -> EditProfilePresenter(repositories.owner, scope) }
        val fields = OwnerFormFields(view, presenter::changeInput)
        val save = view.findViewById<MaterialButton>(R.id.save)
        val message = view.findViewById<TextView>(R.id.form_message)
        save.setOnClickListener {
            presenter.submit()
            fields.focusError(presenter.state.value.errors)
        }
        renderState(presenter.state) { state ->
            fields.render(state.input, state.errors, state.hasOwner && !state.loading && !state.saving)
            save.isEnabled = !state.loading && !state.saving
            save.setText(when {
                state.loading -> R.string.profile_loading
                state.saving -> R.string.profile_saving
                state.loadFailed -> R.string.retry
                !state.hasOwner -> R.string.create_account
                else -> R.string.save_changes
            })
            view.findViewById<View>(R.id.back).isEnabled = !state.saving
            val messageId = state.repositoryError?.stringResource() ?: when {
                state.loading -> R.string.profile_loading
                !state.hasOwner -> R.string.local_profile_missing
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            if (state.saved || state.openRegistration) whenResumed(view) {
                if (presenter.takeSaved()) {
                    Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
                    navigator.back()
                } else if (presenter.takeRegistration()) {
                    navigator.openRoot(Screen.REGISTRATION)
                }
            }
        }
    }
}