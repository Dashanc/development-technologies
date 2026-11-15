package com.example.mobile.data.remote

import java.util.concurrent.ConcurrentHashMap

class RemoteSession(accessToken: String, ownerId: String) {
    val ownerId: String = ownerId.canonicalId()
    private val token = accessToken.also { require(it.matches(Regex("[a-f0-9]{64}"))) }
    internal val metadata = RemoteMetadata()

    internal fun authorization() = "Bearer $token"
}

internal class RemoteMetadata {
    val pets = ConcurrentHashMap.newKeySet<String>()
    val records = ConcurrentHashMap<String, RecordLocation>()
    val photos = ConcurrentHashMap<String, String>()
}

internal data class RecordLocation(val petId: String, val collection: String) {
    fun path(id: String) = listOf("pets", petId, collection, id)
}