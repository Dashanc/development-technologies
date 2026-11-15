package com.example.mobile.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.data.local.model.LocalDataTransfer
import com.example.mobile.data.local.model.LocalDataTransfers
import com.example.mobile.domain.repository.RepositoryError
import java.time.DateTimeException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class LocalDataTransferStorage(private val preferences: LocalPreferences) {
    private val key = stringPreferencesKey("local_data_transfers_v1")
    private val json = Json { encodeDefaults = true; classDiscriminator = "type" }
    private val passport = PassportCodec()

    suspend fun read() = preferences.observe { decode(it[key]) }.first()

    suspend fun save(transfer: LocalDataTransfer, active: () -> Boolean) = preferences.edit { values ->
        if (!active()) throw LocalDataException(RepositoryError.AUTHENTICATION_REQUIRED)
        val current = decode(values[key])
        val next = current.copy(transfers = current.transfers.filterNot {
            it.server == transfer.server && it.ownerId == transfer.ownerId && it.snapshot.owner?.id == transfer.snapshot.owner?.id
        } + transfer)
        validate(next)
        values[key] = json.encodeToString(next)
    }

    private fun decode(text: String?): LocalDataTransfers {
        if (text == null) return LocalDataTransfers()
        try {
            val document = json.parseToJsonElement(text)
            val version = document.jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull
                ?: throw LocalDataException(RepositoryError.CORRUPT_DATA)
            if (version != 1) throw LocalDataException(RepositoryError.UNSUPPORTED_SCHEMA)
            return json.decodeFromJsonElement(LocalDataTransfers.serializer(), document).also(::validate)
        } catch (_: SerializationException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        } catch (_: IllegalArgumentException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        } catch (_: DateTimeException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        }
    }

    private fun validate(data: LocalDataTransfers) {
        require(data.schemaVersion == 1)
        require(data.transfers.map { Triple(it.server, it.ownerId, it.snapshot.owner?.id) }.toSet().size == data.transfers.size)
        data.transfers.forEach(::validate)
    }

    private fun validate(transfer: LocalDataTransfer) {
        require(transfer.id.isNotBlank() && transfer.reviewId.isNotBlank() && transfer.server.isNotBlank() && transfer.ownerId.isNotBlank())
        require(transfer.serverOwner.id == transfer.ownerId && transfer.ownerRevision.isNotBlank())

        val snapshot = transfer.snapshot
        require(snapshot.owner != null && snapshot.pets.all { it.photoUri == null })
        passport.validate(snapshot)

        val pets = snapshot.pets.associateBy { it.id }
        val records = snapshot.records.associateBy { it.id }
        require(pets.keys.containsAll(transfer.petIds.keys))
        require(records.keys.containsAll(transfer.recordIds.keys))
        require(transfer.petIds.values.none(String::isBlank) && transfer.recordIds.values.none(String::isBlank))
        require(transfer.petIds.values.toSet().size == transfer.petIds.size)
        require(transfer.recordIds.values.toSet().size == transfer.recordIds.size)
        require(transfer.recordIds.keys.all { records.getValue(it).petId in transfer.petIds })

        transfer.pending?.let { pending ->
            require(!transfer.reviewRequired && pending.startedAt > 0)
            val item = pending.itemId
            require(when {
                item.startsWith("pet:") -> item.removePrefix("pet:").let { it in pets && it !in transfer.petIds }
                item.startsWith("record:") -> item.removePrefix("record:").let { it in records && it !in transfer.recordIds }
                else -> false
            })
        }

        if (transfer.completed) {
            require(transfer.approved && !transfer.reviewRequired && transfer.pending == null)
            require(!transfer.includeOwner || transfer.ownerTransferred)
            require(transfer.petIds.size == pets.size && transfer.recordIds.size == records.size)
        }
    }
}