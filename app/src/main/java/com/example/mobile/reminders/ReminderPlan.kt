package com.example.mobile.reminders

import com.example.mobile.domain.model.AppSettings
import com.example.mobile.presenter.health.HealthData
import com.example.mobile.presenter.health.UpcomingHealthEvent
import com.example.mobile.presenter.health.upcomingEvents
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal data class ReminderPlan(val identity: String, val event: UpcomingHealthEvent, val triggerAt: Instant)

internal fun reminderPlans(data: HealthData, settings: AppSettings, today: LocalDate, zone: ZoneId): List<ReminderPlan> {
    val pets = data.pets.associateBy { it.id }
    return data.items.upcomingEvents(today).mapNotNull { event ->
        val pet = pets[event.petId] ?: return@mapNotNull null
        val identity = listOf(pet.ownerId, pet.id, event.recordId, event.type.name).joinToString("/")
        val at = event.date.minusDays(settings.reminderLeadTime.days.toLong()).atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()
        ReminderPlan(identity, event, at)
    }
}