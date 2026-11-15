package com.example.mobile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mobile.domain.model.AccountMode
import com.example.mobile.presenter.DataStatusPresenter
import com.example.mobile.ui.common.DataStatusBanner
import com.example.mobile.ui.navigation.AppNavigator
import com.example.mobile.ui.navigation.NavigationHost
import com.example.mobile.ui.navigation.ScreenNavigator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationHost {
    private lateinit var appNavigator: AppNavigator

    override val navigator: ScreenNavigator
        get() = appNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, keyboard.bottom),
            )
            WindowInsetsCompat.CONSUMED
        }
        appNavigator = AppNavigator(this, findViewById(R.id.bottom_navigation))
        appNavigator.attach(savedInstanceState == null)
        val repositories = (application as MurPassportApplication).repositories
        DataStatusBanner(findViewById(R.id.main), this, DataStatusPresenter(repositories.dataStatus, lifecycleScope))
        val container = findViewById<android.view.View>(R.id.screen_container)
        val authentication = repositories.authentication
        if (authentication.session.value.mode == AccountMode.RESTORING) container.visibility = android.view.View.INVISIBLE
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                authentication.session.collect { session ->
                    if (session.mode == AccountMode.SIGNED_OUT) appNavigator.requireAuthentication()
                    if (session.mode == AccountMode.RESTORING) appNavigator.requireAuthentication(resumeProfile = true)
                    container.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as MurPassportApplication).refreshSettings()
    }
}