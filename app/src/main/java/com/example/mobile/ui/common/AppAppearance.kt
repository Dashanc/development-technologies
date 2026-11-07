package com.example.mobile.ui.common

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import com.example.mobile.domain.model.AppTheme
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class AppAppearance(private val context: Context, private val settings: SettingsRepository) {
    private var observation: Job? = null

    fun start(scope: CoroutineScope) {
        if (observation?.isActive == true) return
        observation = scope.launch {
            settings.observeSettings().collect { result ->
                if (result is RepositoryResult.Success) {
                    val value = result.value
                    AppCompatDelegate.setDefaultNightMode(when (value.theme) {
                        AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    })
                    val manager = context.getSystemService(LocaleManager::class.java)
                    val locales = LocaleList.forLanguageTags(value.language.languageTag)
                    if (manager.applicationLocales != locales) manager.applicationLocales = locales
                }
            }
        }
    }
}