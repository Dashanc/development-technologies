package com.example.mobile.ui.pets

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class PetProfileFragment : ScreenFragment(R.layout.fragment_pet_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.log_weight, Screen.WEIGHT_ENTRY)
        view.link(R.id.add_event, Screen.VACCINATION_ENTRY)
        view.link(R.id.history, Screen.HEALTH_HISTORY)
        view.link(R.id.upcoming_event, Screen.VET_VISIT_ENTRY)
    }
}