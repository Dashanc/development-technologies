package com.example.mobile.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.domain.repository.RepositoryError
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
internal data class ReminderLedger(
    val schemaVersion: Int = 1,
    val scheduled: Map<String, Long> = emptyMap(),
    val delivered: Map<String, String> = emptyMap(),
)

internal class LocalReminderStorage(private val preferences: LocalPreferences) {
    private val key = stringPreferencesKey("reminder_ledger_v1")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read() = preferences.observe { values ->
        val state = try {
            values[key]?.let { json.decodeFromString<ReminderLedger>(it) } ?: ReminderLedger()
        } catch (_: SerializationException) {
            throw LocalDataException(RepositoryError.CORRUPT_DATA)
        }
        if (state.schemaVersion != 1) throw LocalDataException(RepositoryError.UNSUPPORTED_SCHEMA)
        state
    }.first()

    suspend fun write(state: ReminderLedger) = preferences.edit { it[key] = json.encodeToString(state) }
}