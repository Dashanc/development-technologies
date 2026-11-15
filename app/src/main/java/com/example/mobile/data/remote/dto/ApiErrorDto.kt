package com.example.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ApiErrorEnvelope(val error: ApiErrorDto)

@Serializable
internal data class ApiErrorDto(val code: String, val fields: Map<String, String> = emptyMap())