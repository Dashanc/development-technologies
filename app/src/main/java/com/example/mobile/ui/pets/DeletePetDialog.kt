package com.example.mobile.ui.pets

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mobile.MurPassportApplication
import com.example.mobile.R
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.presenter.pets.DeletePetPresenter
import com.example.mobile.presenter.pets.DeletePetState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.navigation.NavigationHost
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class DeletePetDialog : DialogFragment() {
    private lateinit var presenter: DeletePetPresenter
    private var leaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as MurPassportApplication
        presenter = retainedPresenter { scope ->
            DeletePetPresenter(app.repositories.owner, app.repositories.pets, app.repositories.petPhotos,
                app.reminders, arguments?.getString(Screen.ARG_PET_ID), scope)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { presenter.state.collect(::render) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pet_delete_title)
            .setMessage(getString(R.string.pet_delete_message, requireArguments().getString(ARG_NAME)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.pet_delete_confirm, null)
            .create()

    override fun onStart() {
        super.onStart()
        val alert = requireDialog() as AlertDialog
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val state = presenter.state.value
            when {
                state.missing -> closeProfile(R.string.pet_not_found)
                state.pet == null -> presenter.retry()
                else -> presenter.confirmDelete()
            }
        }
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            if (presenter.state.value.deleted) closeProfile() else dismiss()
        }
        render(presenter.state.value)
    }

    private fun render(state: DeletePetState) {
        val alert = dialog as? AlertDialog ?: return
        isCancelable = !state.deleting && !state.deleted && !state.missing
        val messageId = when {
            state.deleting -> R.string.pet_deleting
            state.cleanupFailed -> R.string.pet_delete_cleanup_error
            state.error == RepositoryError.CORRUPT_DATA -> R.string.error_local_data_corrupt
            state.error == RepositoryError.UNSUPPORTED_SCHEMA -> R.string.error_local_schema
            state.error != null -> if (state.pet == null) R.string.error_pet_read else R.string.error_pet_delete
            state.loading -> R.string.pet_loading
            state.missing -> R.string.pet_not_found
            else -> null
        }
        alert.setMessage(messageId?.let(::getString) ?: getString(R.string.pet_delete_message, state.pet?.name))
        alert.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            isEnabled = !state.loading && !state.deleting && !state.finished
            setText(when {
                state.missing -> R.string.pet_back_to_list
                state.cleanupFailed || state.pet == null -> R.string.retry
                else -> R.string.pet_delete_confirm
            })
        }
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
            visibility = if (state.missing) View.GONE else View.VISIBLE
            isEnabled = !state.deleting && !state.finished
            setText(if (state.deleted) R.string.pet_back_to_list else R.string.cancel)
        }
        if (state.finished && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) closeProfile(R.string.pet_deleted)
    }

    private fun closeProfile(messageId: Int? = null) {
        if (leaving || parentFragmentManager.isStateSaved) return
        leaving = true
        messageId?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        (requireActivity() as NavigationHost).navigator.openRoot(Screen.PET_LIST)
    }

    companion object {
        private const val TAG = "delete_pet"
        private const val ARG_NAME = "pet_name"

        fun show(fragment: Fragment, petId: String, petName: String) {
            val manager = fragment.childFragmentManager
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
            DeletePetDialog().apply {
                arguments = Bundle().apply {
                    putString(Screen.ARG_PET_ID, petId)
                    putString(ARG_NAME, petName)
                }
            }.showNow(manager, TAG)
        }
    }
}