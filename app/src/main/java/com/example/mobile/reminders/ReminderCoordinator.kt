package com.example.mobile.reminders

import android.content.Context
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.data.local.ReminderLedger
import com.example.mobile.domain.model.AppSettings
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.presenter.health.HealthData
import com.example.mobile.presenter.health.observeHealthData
import com.example.mobile.presenter.owner.ReminderStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ReminderCoordinator(context: Context, private val repositories: LocalRepositories, private val scope: CoroutineScope) {
    private val alarms = ReminderAlarms(context)
    private val notifications = ReminderNotifications(context)
    private val storage = repositories.reminderStorage
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow(ReminderStatus.READY)
    val status = mutableStatus.asStateFlow()
    private var observation: Job? = null

    fun start() {
        observation?.cancel()
        observation = scope.launch { snapshots().collect { snapshot -> mutex.withLock { reconcile(snapshot) } } }
    }

    fun refresh() {
        start()
    }

    suspend fun rebuild(reset: Boolean = false) {
        mutex.withLock { reconcile(snapshots().first(), reset) }
    }

    suspend fun deliver(identity: String, triggerAt: Long) {
        mutex.withLock {
            val snapshot = snapshots().first()
            val (data, settings) = snapshot
            if (data.error != null || settings == null || !settings.remindersEnabled || !notifications.allowed()) {
                reconcile(snapshot)
                return
            }
            val ledger = readLedger() ?: return
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            val plan = reminderPlans(data, settings, LocalDate.now(zone), zone).find {
                it.identity == identity && it.triggerAt.toEpochMilli() == triggerAt
            }
            val valid = plan != null && ledger.scheduled[identity] == triggerAt && !now.isBefore(plan.triggerAt) &&
                now.atZone(zone).toLocalDate() == plan.triggerAt.atZone(zone).toLocalDate() &&
                ledger.delivered[identity] != plan.event.date.toString()
            if (valid) {
                val next = ledger.copy(scheduled = ledger.scheduled - identity, delivered = ledger.delivered + (identity to plan.event.date.toString()))
                if (!writeLedger(next)) return
                try {
                    notifications.show(plan, settings.language)
                } catch (_: SecurityException) {
                    mutableStatus.value = ReminderStatus.SCHEDULE_FAILED
                    return
                }
            }
            reconcile(snapshot)
        }
    }

    private fun snapshots() = combine(
        observeHealthData(repositories.owner, repositories.pets, repositories.medicalRecords),
        repositories.settings.observeSettings(),
    ) { data, settings -> data to (settings as? RepositoryResult.Success)?.value }

    private suspend fun reconcile(snapshot: Pair<HealthData, AppSettings?>, reset: Boolean = false) {
        val (data, settings) = snapshot
        val ledger = readLedger() ?: return
        if (data.error != null || settings == null) {
            ledger.scheduled.keys.forEach(alarms::cancel)
            mutableStatus.value = ReminderStatus.READ_FAILED
            return
        }
        notifications.configure(settings.language)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = Instant.now()
        val plans = reminderPlans(data, settings, today, zone)
        val delivered = ledger.delivered.filter { (key, date) -> plans.any { it.identity == key && it.event.date.toString() == date } }
        val active = if (!settings.remindersEnabled || !notifications.allowed()) emptyList() else plans.filter { plan ->
            delivered[plan.identity] != plan.event.date.toString() && (plan.triggerAt.isAfter(now) ||
                !reset && ledger.scheduled[plan.identity] == plan.triggerAt.toEpochMilli() && alarms.exists(plan.identity) &&
                plan.triggerAt.atZone(zone).toLocalDate() == today)
        }
        val scheduled = active.associate { it.identity to it.triggerAt.toEpochMilli() }
        (ledger.scheduled.keys - scheduled.keys).forEach(alarms::cancel)
        ((ledger.delivered.keys + ledger.scheduled.keys) - plans.map { it.identity }.toSet()).forEach(notifications::cancel)
        (ledger.delivered.keys - delivered.keys).forEach(notifications::cancel)
        if (!settings.remindersEnabled || !notifications.allowed()) {
            (ledger.delivered.keys + ledger.scheduled.keys).forEach(notifications::cancel)
        }
        if (!writeLedger(ReminderLedger(scheduled = scheduled, delivered = delivered))) return
        try {
            active.filter { it.triggerAt.isAfter(now) }.forEach(alarms::schedule)
            mutableStatus.value = ReminderStatus.READY
        } catch (_: SecurityException) {
            mutableStatus.value = ReminderStatus.SCHEDULE_FAILED
        } catch (_: IllegalArgumentException) {
            mutableStatus.value = ReminderStatus.SCHEDULE_FAILED
        } catch (_: IllegalStateException) {
            mutableStatus.value = ReminderStatus.SCHEDULE_FAILED
        }
    }

    private suspend fun readLedger(): ReminderLedger? = when (val result = storage.read()) {
        is RepositoryResult.Success -> result.value
        is RepositoryResult.Failure -> null.also { mutableStatus.value = ReminderStatus.READ_FAILED }
    }

    private suspend fun writeLedger(ledger: ReminderLedger): Boolean = when (storage.write(ledger)) {
        is RepositoryResult.Success -> true
        is RepositoryResult.Failure -> false.also { mutableStatus.value = ReminderStatus.WRITE_FAILED }
    }
}