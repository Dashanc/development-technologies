package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.InformationDialog
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class SettingsFragment : ScreenFragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.logout, Screen.REGISTRATION)
        view.findViewById<View>(R.id.terms).setOnClickListener {
            InformationDialog.show(this, R.string.terms, R.string.terms_unavailable)
        }
        view.findViewById<View>(R.id.privacy).setOnClickListener {
            InformationDialog.show(this, R.string.privacy, R.string.privacy_unavailable)
        }
        view.findViewById<View>(R.id.about).setOnClickListener {
            InformationDialog.show(this, R.string.about, R.string.about_description)
        }
    }
}