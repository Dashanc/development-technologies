package com.example.mobile.ui.navigation

import com.example.mobile.domain.model.Pet

interface ScreenNavigator {
    fun open(screen: Screen, petId: String? = null)
    fun editPet(pet: Pet)
    fun replace(screen: Screen, petId: String? = null)
    fun openRoot(screen: Screen)
    fun back()
}

interface NavigationHost {
    val navigator: ScreenNavigator
}