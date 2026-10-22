package com.example.mobile.domain.model

data class AppSettings(
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val theme: AppTheme = AppTheme.SYSTEM,
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
    val remindersEnabled: Boolean = false,
    val reminderLeadTime: ReminderLeadTime = ReminderLeadTime.ONE_DAY,
)

enum class AppLanguage { RUSSIAN, ENGLISH, KAZAKH }

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class WeightUnit { KILOGRAM, POUND }

enum class ReminderLeadTime(val days: Int) {
    SAME_DAY(0), ONE_DAY(1), THREE_DAYS(3),
}