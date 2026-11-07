package com.example.mobile.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.mobile.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InformationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireArguments().getInt(ARG_TITLE))
            .setMessage(requireArguments().getInt(ARG_MESSAGE))
            .setPositiveButton(R.string.done, null)
            .create()

    companion object {
        private const val TAG = "information"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"

        fun show(fragment: Fragment, @StringRes title: Int, @StringRes message: Int) {
            val manager = fragment.childFragmentManager
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
            InformationDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE, title)
                    putInt(ARG_MESSAGE, message)
                }
            }.showNow(manager, TAG)
        }
    }
}