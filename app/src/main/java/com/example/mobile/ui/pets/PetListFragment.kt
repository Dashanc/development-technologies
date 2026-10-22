package com.example.mobile.ui.pets

import android.os.Bundle
import android.view.View
import com.example.mobile.R
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen

class PetListFragment : ScreenFragment(R.layout.fragment_pet_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.add_top, Screen.ADD_PET)
        view.link(R.id.add_pet, Screen.ADD_PET)
    }
}