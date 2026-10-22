package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.InformationDialog
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class RegistrationFragment : ScreenFragment(R.layout.fragment_registration) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.register).setOnClickListener {
            showSavingUnavailable()
            navigator.openRoot(Screen.OWNER_PROFILE)
        }
        view.link(R.id.sign_in, Screen.OWNER_PROFILE)
        view.findViewById<View>(R.id.terms).setOnClickListener {
            InformationDialog.show(this, R.string.terms, R.string.terms_unavailable)
        }
        view.findViewById<View>(R.id.privacy).setOnClickListener {
            InformationDialog.show(this, R.string.privacy, R.string.privacy_unavailable)
        }
    }
}