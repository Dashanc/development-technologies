package com.example.mobile.ui.common

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.example.mobile.R
import com.example.mobile.ui.navigation.NavigationHost
import com.example.mobile.ui.navigation.Screen

abstract class ScreenFragment(@LayoutRes layout: Int) : Fragment(layout) {
    protected val navigator get() = (requireActivity() as NavigationHost).navigator
    protected val petId: String? get() = arguments?.getString(Screen.ARG_PET_ID)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.back)?.setOnClickListener { navigator.back() }
    }

    protected fun View.link(@IdRes id: Int, screen: Screen) {
        findViewById<View>(id).setOnClickListener { navigator.open(screen, petId) }
    }

    protected fun View.returnWithoutSaving(@IdRes id: Int) {
        findViewById<View>(id).setOnClickListener {
            showSavingUnavailable()
            navigator.back()
        }
    }

    protected fun showSavingUnavailable() {
        Toast.makeText(requireContext(), R.string.saving_unavailable, Toast.LENGTH_SHORT).show()
    }
}