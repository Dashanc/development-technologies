package com.example.mobile.domain.model

data class AppSettings(
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val theme: AppTheme = AppTheme.SYSTEM,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
    val remindersEnabled: Boolean = false,
    val reminderLeadTime: ReminderLeadTime = ReminderLeadTime.ONE_DAY,
)

enum class AppLanguage(val languageTag: String) { RUSSIAN("ru"), ENGLISH("en"), KAZAKH("kk") }

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class WeightUnit {
    KILOGRAM, POUND;

    fun fromKilograms(value: Double) = if (this == POUND) value / 0.45359237 else value
    fun toKilograms(value: Double) = if (this == POUND) value * 0.45359237 else value
}

enum class ReminderLeadTime(val days: Int) {
    SAME_DAY(0), ONE_DAY(1), THREE_DAYS(3),
}