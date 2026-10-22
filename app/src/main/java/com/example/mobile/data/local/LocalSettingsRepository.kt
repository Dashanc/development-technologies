package com.example.mobile.data.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.model.AppSettings
import com.example.mobile.domain.model.AppTheme
import com.example.mobile.domain.model.ReminderLeadTime
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.SettingsRepository

internal class LocalSettingsRepository(private val preferences: LocalPreferences) : SettingsRepository {
    private val languageKey = stringPreferencesKey("settings_language_v1")
    private val themeKey = stringPreferencesKey("settings_theme_v1")
    private val weightUnitKey = stringPreferencesKey("settings_weight_unit_v1")
    private val remindersKey = booleanPreferencesKey("settings_reminders_enabled_v1")
    private val leadDaysKey = intPreferencesKey("settings_reminder_lead_days_v1")

    private val languages = mapOf(
        AppLanguage.RUSSIAN to "ru", AppLanguage.ENGLISH to "en", AppLanguage.KAZAKH to "kk",
    )
    private val themes = mapOf(AppTheme.SYSTEM to "system", AppTheme.LIGHT to "light", AppTheme.DARK to "dark")
    private val weightUnits = mapOf(WeightUnit.KILOGRAM to "kg", WeightUnit.POUND to "lb")

    override fun observeSettings() = preferences.observe { values ->
        AppSettings(
            language = values.decode(languageKey, languages, AppLanguage.RUSSIAN),
            theme = values.decode(themeKey, themes, AppTheme.SYSTEM),
            weightUnit = values.decode(weightUnitKey, weightUnits, WeightUnit.KILOGRAM),
            remindersEnabled = values[remindersKey] ?: false,
            reminderLeadTime = values[leadDaysKey]?.let { days ->
                ReminderLeadTime.entries.find { it.days == days }
                    ?: throw LocalDataException(RepositoryError.CORRUPT_DATA)
            } ?: ReminderLeadTime.ONE_DAY,
        )
    }

    override suspend fun setLanguage(language: AppLanguage) = preferences.edit {
        it[languageKey] = languages.getValue(language)
    }

    override suspend fun setTheme(theme: AppTheme) = preferences.edit {
        it[themeKey] = themes.getValue(theme)
    }

    override suspend fun setWeightUnit(unit: WeightUnit) = preferences.edit {
        it[weightUnitKey] = weightUnits.getValue(unit)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) = preferences.edit {
        it[remindersKey] = enabled
    }

    override suspend fun setReminderLeadTime(leadTime: ReminderLeadTime) = preferences.edit {
        it[leadDaysKey] = leadTime.days
    }

    private fun <T> Preferences.decode(key: Preferences.Key<String>, codes: Map<T, String>, default: T): T {
        val code = this[key] ?: return default
        return codes.entries.find { it.value == code }?.key
            ?: throw LocalDataException(RepositoryError.CORRUPT_DATA)
    }
}