package com.example.mobile.ui.owner

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.mobile.domain.model.AppLanguage

internal object RegistrationLanguage {
    val options = listOf(AppLanguage.RUSSIAN, AppLanguage.ENGLISH, AppLanguage.KAZAKH)

    fun apply(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.languageTag)
        if (AppCompatDelegate.getApplicationLocales() == locales) return
        AppCompatDelegate.setApplicationLocales(locales)
    }
}