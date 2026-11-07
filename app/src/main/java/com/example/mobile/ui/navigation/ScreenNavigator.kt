package com.example.mobile.ui.navigation

interface ScreenNavigator {
    fun open(screen: Screen, petId: String? = null)
    fun replace(screen: Screen, petId: String? = null)
    fun openRoot(screen: Screen)
    fun back()
}

interface NavigationHost {
    val navigator: ScreenNavigator
}