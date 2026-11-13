package com.example.mobile.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
internal data class AccountPhotos(val schemaVersion: Int = 1, val ownerId: String? = null, val photos: Map<String, String> = emptyMap())

internal class AccountPhotoStorage(private val preferences: LocalPreferences) {
    private val key = stringPreferencesKey("account_photos_v1")
    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(value: String?): AccountPhotos {
        val data = try {
            value?.let { json.decodeFromString<AccountPhotos>(it) } ?: AccountPhotos()
        } catch (_: SerializationException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        }
        if (data.schemaVersion != 1) throw LocalDataException(RepositoryError.UNSUPPORTED_SCHEMA)
        return data
    }

    suspend fun read() = preferences.observe { decode(it[key]) }.first()

    suspend fun save(ownerId: String, petId: String, uri: String?) = preferences.edit { values ->
        val current = decode(values[key])
        if (current.ownerId != null && current.ownerId != ownerId) throw LocalDataException(RepositoryError.IDENTITY_CONFLICT)
        values[key] = json.encodeToString(current.copy(ownerId = ownerId,
            photos = if (uri == null) current.photos - petId else current.photos + (petId to uri)))
    }

    suspend fun clear() = preferences.edit { it.remove(key) }
}