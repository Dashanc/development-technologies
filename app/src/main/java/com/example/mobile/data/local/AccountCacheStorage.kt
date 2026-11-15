package com.example.mobile.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.data.local.model.CachedPassport
import com.example.mobile.domain.repository.RepositoryError
import java.time.DateTimeException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class AccountCacheStorage(private val preferences: LocalPreferences) {
    private val key = stringPreferencesKey("server_cache_v1")
    private val json = Json { encodeDefaults = true; classDiscriminator = "type" }

    fun observe(ownerId: String, server: String) = preferences.observe { values ->
        decode(values[key])?.takeIf { it.ownerId == ownerId && it.server == server }
    }

    suspend fun read(ownerId: String, server: String) = observe(ownerId, server).first()

    suspend fun update(ownerId: String, server: String, active: () -> Boolean, transform: (CachedPassport) -> CachedPassport) =
        preferences.edit { values ->
            if (!active()) throw LocalDataException(RepositoryError.AUTHENTICATION_REQUIRED)
            val current = decode(values[key])
            if (current != null && (current.ownerId != ownerId || current.server != server)) {
                throw LocalDataException(RepositoryError.IDENTITY_CONFLICT)
            }
            val next = transform(current ?: CachedPassport(server = server, ownerId = ownerId))
            validate(next)
            values[key] = json.encodeToString(next)
        }

    suspend fun clear() = preferences.edit { it.remove(key) }

    private fun decode(text: String?): CachedPassport? {
        if (text == null) return null
        try {
            val document = json.parseToJsonElement(text)
            val version = document.jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull
                ?: throw LocalDataException(RepositoryError.CORRUPT_DATA)
            if (version != 1) throw LocalDataException(RepositoryError.UNSUPPORTED_SCHEMA)
            return json.decodeFromJsonElement(CachedPassport.serializer(), document).also(::validate)
        } catch (_: SerializationException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        } catch (_: IllegalArgumentException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        } catch (_: DateTimeException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        }
    }

    private fun validate(data: CachedPassport) {
        require(data.schemaVersion == 1 && data.server.isNotBlank() && data.ownerId.isNotBlank())
        require(data.owner == null || data.owner.value.id == data.ownerId)
        data.owner?.toDomain()
        data.pets.forEach { (id, pet) ->
            require(id == pet.value.id && pet.value.ownerId == data.ownerId && pet.value.photoUri == null)
            pet.toDomain(null)
        }
        data.records.forEach { (id, record) ->
            require(id == record.value.id && id.isNotBlank() && record.value.petId.isNotBlank())
            record.toDomain()
        }
        require(data.missingPets.intersect(data.pets.keys).isEmpty())
        require(data.missingRecords.intersect(data.records.keys).isEmpty())
        require(data.records.values.none { it.value.petId in data.missingPets })
        require(data.recordsLoaded.all { it in data.pets })
        if (data.petsLoaded) {
            require(data.records.values.all { it.value.petId in data.pets })
        }
    }
}