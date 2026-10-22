package com.example.mobile.data.local

import com.example.mobile.data.local.model.LocalPassportData
import com.example.mobile.domain.repository.RepositoryError
import java.time.DateTimeException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class PassportCodec {
    private val json = Json {
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun decode(value: String?): LocalPassportData {
        if (value == null) return LocalPassportData()
        try {
            val element = json.parseToJsonElement(value)
            val version = element.jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull
                ?: throw LocalDataException(RepositoryError.CORRUPT_DATA)
            if (version != 1) throw LocalDataException(RepositoryError.UNSUPPORTED_SCHEMA)
            return json.decodeFromJsonElement(LocalPassportData.serializer(), element).also(::validate)
        } catch (_: SerializationException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        } catch (_: IllegalArgumentException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        } catch (_: DateTimeException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        }
    }

    fun encode(data: LocalPassportData): String {
        validate(data)
        return json.encodeToString(LocalPassportData.serializer(), data)
    }

    private fun validate(data: LocalPassportData) {
        require(data.schemaVersion == 1)
        require(data.owner == null || data.owner.id.isNotBlank())
        val petIds = data.pets.map { it.id }.toSet()
        require(petIds.size == data.pets.size && petIds.none(String::isBlank))
        require(data.records.map { it.id }.toSet().size == data.records.size)
        data.pets.forEach { pet ->
            require(data.owner != null && pet.ownerId == data.owner.id)
            pet.toDomain()
        }
        data.records.forEach { record ->
            require(record.id.isNotBlank() && record.petId in petIds)
            record.toDomain()
        }
    }
}