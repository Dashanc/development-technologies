package com.example.mobile.domain.repository

import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.model.AppSettings
import com.example.mobile.domain.model.AppTheme
import com.example.mobile.domain.model.ReminderLeadTime
import com.example.mobile.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<RepositoryResult<AppSettings>>
    suspend fun setLanguage(language: AppLanguage): RepositoryResult<Unit>
    suspend fun setTheme(theme: AppTheme): RepositoryResult<Unit>
    suspend fun setWeightUnit(unit: WeightUnit): RepositoryResult<Unit>
    suspend fun setRemindersEnabled(enabled: Boolean): RepositoryResult<Unit>
    suspend fun setReminderLeadTime(leadTime: ReminderLeadTime): RepositoryResult<Unit>
}