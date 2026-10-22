package com.example.mobile.ui.navigation

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.mobile.R
import com.example.mobile.ui.health.HealthHistoryFragment
import com.example.mobile.ui.health.HealthOverviewFragment
import com.example.mobile.ui.health.VaccinationEntryFragment
import com.example.mobile.ui.health.VetVisitEntryFragment
import com.example.mobile.ui.health.WeightEntryFragment
import com.example.mobile.ui.owner.EditProfileFragment
import com.example.mobile.ui.owner.OwnerProfileFragment
import com.example.mobile.ui.owner.RegistrationFragment
import com.example.mobile.ui.owner.SettingsFragment
import com.example.mobile.ui.pets.AddPetFragment
import com.example.mobile.ui.pets.PetListFragment
import com.example.mobile.ui.pets.PetProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AppNavigator(
    private val activity: AppCompatActivity,
    private val bottomBar: BottomNavigationView,
) : ScreenNavigator {
    private val fragments get() = activity.supportFragmentManager
    private var navigationPending = false
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = back()
    }

    fun attach(firstLaunch: Boolean) {
        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
        bottomBar.setOnItemSelectedListener { item ->
            val screen = Screen.entries.first { it.tabId == item.itemId }
            if (currentScreen() != screen) openRoot(screen)
            true
        }
        bottomBar.setOnItemReselectedListener { }
        fragments.addOnBackStackChangedListener(::updateNavigation)
        fragments.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                navigationPending = false
                updateNavigation()
            }
        }, false)
        if (firstLaunch) openRoot(Screen.REGISTRATION) else updateNavigation()
    }

    override fun open(screen: Screen, petId: String?) {
        if (screen.isTab || screen == Screen.REGISTRATION) {
            openRoot(screen)
            return
        }
        if (!canNavigate()) return
        val current = fragments.findFragmentById(R.id.screen_container)
        if (currentScreen() == screen && current?.arguments?.getString(Screen.ARG_PET_ID) == petId) return
        commit(screen, petId, addToBackStack = true)
    }

    override fun replace(screen: Screen, petId: String?) {
        if (screen.isTab || screen == Screen.REGISTRATION) {
            openRoot(screen)
            return
        }
        if (!canNavigate() || currentScreen() == screen) return
        if (fragments.backStackEntryCount > 0) fragments.popBackStackImmediate()
        commit(screen, petId, addToBackStack = true)
    }

    override fun openRoot(screen: Screen) {
        require(screen.isTab || screen == Screen.REGISTRATION)
        if (!canNavigate()) return
        if (currentScreen() == screen && fragments.backStackEntryCount == 0) return
        fragments.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        commit(screen, null, addToBackStack = false)
    }

    override fun back() {
        if (!canNavigate()) return
        hideKeyboard()
        if (fragments.backStackEntryCount > 0) {
            navigationPending = true
            fragments.popBackStack()
        } else if (currentScreen()?.isTab == true && currentScreen() != Screen.PET_LIST) {
            openRoot(Screen.PET_LIST)
        } else {
            activity.finish()
        }
    }

    private fun canNavigate(): Boolean = !navigationPending && !fragments.isStateSaved

    private fun commit(screen: Screen, petId: String?, addToBackStack: Boolean) {
        hideKeyboard()
        val fragment = createFragment(screen).apply {
            arguments = Bundle().apply {
                putString(Screen.ARG_SCREEN, screen.name)
                putString(Screen.ARG_PET_ID, petId)
            }
        }
        navigationPending = true
        fragments.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.screen_container, fragment)
            .setPrimaryNavigationFragment(fragment)
            .apply { if (addToBackStack) addToBackStack(screen.name) }
            .commit()
    }

    private fun updateNavigation() {
        val screen = currentScreen()
        bottomBar.visibility = if (screen?.isTab == true) View.VISIBLE else View.GONE
        if (screen?.isTab == true) bottomBar.menu.findItem(screen.tabId).isChecked = true
        backCallback.isEnabled = fragments.backStackEntryCount > 0 ||
            (screen?.isTab == true && screen != Screen.PET_LIST)
    }

    private fun currentScreen(): Screen? = fragments.findFragmentById(R.id.screen_container)
        ?.arguments?.getString(Screen.ARG_SCREEN)?.let(Screen::valueOf)

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
        activity.currentFocus?.clearFocus()
    }

    private fun createFragment(screen: Screen): Fragment = when (screen) {
        Screen.REGISTRATION -> RegistrationFragment()
        Screen.PET_LIST -> PetListFragment()
        Screen.HEALTH_OVERVIEW -> HealthOverviewFragment()
        Screen.OWNER_PROFILE -> OwnerProfileFragment()
        Screen.EDIT_PROFILE -> EditProfileFragment()
        Screen.SETTINGS -> SettingsFragment()
        Screen.ADD_PET -> AddPetFragment()
        Screen.PET_PROFILE -> PetProfileFragment()
        Screen.HEALTH_HISTORY -> HealthHistoryFragment()
        Screen.WEIGHT_ENTRY -> WeightEntryFragment()
        Screen.VACCINATION_ENTRY -> VaccinationEntryFragment()
        Screen.VET_VISIT_ENTRY -> VetVisitEntryFragment()
    }
}