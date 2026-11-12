package com.example.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class OwnerDto(val id: String, val name: String, val email: String, val phone: String? = null)

@Serializable
internal data class OwnerWriteDto(val name: String, val email: String, val phone: String?)