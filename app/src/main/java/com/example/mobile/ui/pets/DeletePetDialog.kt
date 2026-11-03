package com.example.mobile.ui.pets

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.mobile.R
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeletePetDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pet_delete_title)
            .setMessage(getString(R.string.pet_delete_message, requireArguments().getString(ARG_NAME)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.pet_delete_confirm, null)
            .create()

    override fun onStart() {
        super.onStart()
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
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