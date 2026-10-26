package com.example.mobile.ui.navigation

import androidx.annotation.IdRes
import com.example.mobile.R

enum class Screen(@param:IdRes val tabId: Int = 0) {
    REGISTRATION,
    PET_LIST(R.id.nav_pets),
    HEALTH_OVERVIEW(R.id.nav_health),
    OWNER_PROFILE(R.id.nav_profile),
    EDIT_PROFILE,
    SETTINGS,
    ADD_PET,
    PET_PROFILE,
    HEALTH_HISTORY,
    WEIGHT_ENTRY,
    VACCINATION_ENTRY,
    VET_VISIT_ENTRY;

    val isTab: Boolean get() = tabId != 0

    companion object {
        const val ARG_SCREEN = "screen"
        const val ARG_PET_ID = "pet_id"
        const val ARG_RESUME_PROFILE = "resume_profile"
    }
}