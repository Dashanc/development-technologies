package com.example.mobile.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first

internal class AccountModeStorage(private val preferences: LocalPreferences) {
    private val localMode = booleanPreferencesKey("local_profile_mode_v1")
    suspend fun read() = preferences.observe { it[localMode] ?: false }.first()
    suspend fun setLocal(enabled: Boolean) = preferences.edit { it[localMode] = enabled }
}