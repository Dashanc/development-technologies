package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class HealthOverviewFragment : ScreenFragment(R.layout.fragment_health_overview) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.all_records, Screen.HEALTH_HISTORY)
        view.link(R.id.upcoming_event, Screen.VET_VISIT_ENTRY)
    }
}