package com.example.mobile.domain.repository

interface ReminderScheduleRepository {
    suspend fun refreshSchedule(): RepositoryResult<Unit>
}