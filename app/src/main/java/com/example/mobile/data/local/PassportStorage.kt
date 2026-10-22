package com.example.mobile.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.data.local.model.LocalPassportData
import com.example.mobile.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.Flow

internal class PassportStorage(private val preferences: LocalPreferences) {
    private val key = stringPreferencesKey("passport_document_v1")
    private val codec = PassportCodec()

    fun <T> observe(read: (LocalPassportData) -> T): Flow<RepositoryResult<T>> =
        preferences.observe { read(codec.decode(it[key])) }

    suspend fun update(transform: (LocalPassportData) -> LocalPassportData): RepositoryResult<Unit> =
        preferences.edit { values ->
            val current = codec.decode(values[key])
            values[key] = codec.encode(transform(current))
        }
}