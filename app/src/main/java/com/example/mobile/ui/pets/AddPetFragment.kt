package com.example.mobile.ui.pets

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class AddPetFragment : ScreenFragment(R.layout.fragment_add_pet) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.extra).setOnClickListener {
            if (!childFragmentManager.isStateSaved &&
                childFragmentManager.findFragmentByTag(PetExtraDataSheet.TAG) == null
            ) {
                PetExtraDataSheet().showNow(childFragmentManager, PetExtraDataSheet.TAG)
            }
        }
        view.findViewById<View>(R.id.save).setOnClickListener {
            showSavingUnavailable()
            navigator.replace(Screen.PET_PROFILE)
        }
    }
}