package com.example.mobile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mobile.ui.navigation.AppNavigator
import com.example.mobile.ui.navigation.NavigationHost
import com.example.mobile.ui.navigation.ScreenNavigator

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
    }
}