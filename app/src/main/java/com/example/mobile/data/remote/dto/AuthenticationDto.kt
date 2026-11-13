package com.example.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal class LoginDto(val email: String, val password: String)

@Serializable
internal class RegisterDto(val name: String, val email: String, val phone: String?, val password: String)

@Serializable
internal class AuthenticationDto(val token: String, val tokenType: String, val expiresAt: String, val owner: OwnerDto)

@Serializable
internal class SessionDto(val expiresAt: String, val owner: OwnerDto)