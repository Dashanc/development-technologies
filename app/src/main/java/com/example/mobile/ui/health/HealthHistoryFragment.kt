package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class HealthHistoryFragment : ScreenFragment(R.layout.fragment_health_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.add_record, Screen.WEIGHT_ENTRY)
    }
}