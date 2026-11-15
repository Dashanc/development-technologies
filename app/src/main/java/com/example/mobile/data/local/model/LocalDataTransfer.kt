package com.example.mobile.data.local.model

import kotlinx.serialization.Serializable

@Serializable
internal data class LocalDataTransfers(val schemaVersion: Int = 1, val transfers: List<LocalDataTransfer> = emptyList())

@Serializable
internal data class LocalDataTransfer(
    val id: String,
    val reviewId: String = id,
    val server: String,
    val ownerId: String,
    val snapshot: LocalPassportData,
    val serverOwner: LocalOwner,
    val ownerRevision: String,
    val approved: Boolean = false,
    val includeOwner: Boolean = false,
    val ownerTransferred: Boolean = false,
    val petIds: Map<String, String> = emptyMap(),
    val recordIds: Map<String, String> = emptyMap(),
    val pending: TransferRequest? = null,
    val reviewRequired: Boolean = false,
    val completed: Boolean = false,
)

@Serializable
internal data class TransferRequest(val itemId: String, val startedAt: Long)