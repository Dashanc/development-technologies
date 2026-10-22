package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class OwnerProfileFragment : ScreenFragment(R.layout.fragment_owner_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.edit_profile, Screen.EDIT_PROFILE)
        view.link(R.id.settings, Screen.SETTINGS)
    }
}