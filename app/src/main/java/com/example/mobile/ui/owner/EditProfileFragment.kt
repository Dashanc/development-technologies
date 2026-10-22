package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment

class EditProfileFragment : ScreenFragment(R.layout.fragment_edit_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.returnWithoutSaving(R.id.save)
    }
}