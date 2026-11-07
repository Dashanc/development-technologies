package com.example.mobile.domain.model

data class Owner(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
)