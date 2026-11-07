package com.example.mobile.ui.owner

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.example.mobile.MurPassportApplication
import com.example.mobile.R
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.model.AppTheme
import com.example.mobile.domain.model.ReminderLeadTime
import com.example.mobile.domain.model.WeightUnit
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.presenter.owner.ReminderStatus
import com.example.mobile.presenter.owner.SettingsPresenter
import com.example.mobile.reminders.ReminderNotifications
import com.example.mobile.ui.common.InformationDialog
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.whenResumed
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsFragment : ScreenFragment(R.layout.fragment_settings) {
    private lateinit var presenter: SettingsPresenter
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        presenter.permissionResult(ReminderNotifications(requireContext()).allowed())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as MurPassportApplication
        presenter = retainedPresenter { scope -> SettingsPresenter(app.repositories.settings, app.reminders.status, scope) }
        val languages = AppLanguage.entries
        val themes = AppTheme.entries
        val units = WeightUnit.entries
        val leads = listOf(ReminderLeadTime.ONE_DAY, ReminderLeadTime.SAME_DAY, ReminderLeadTime.THREE_DAYS)
        val language = view.findViewById<Spinner>(R.id.language)
        val theme = view.findViewById<Spinner>(R.id.theme)
        val unit = view.findViewById<Spinner>(R.id.weight_units)
        val lead = view.findViewById<Spinner>(R.id.reminder_time)
        val reminders = view.findViewById<MaterialSwitch>(R.id.reminders)
        var rendering = false
        fun Spinner.bind(changed: (Int) -> Unit) {
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                    if (!rendering && position == selectedItemPosition && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) changed(position)
                }
            }
        }
        language.bind { presenter.changeLanguage(languages[it]) }
        theme.bind { presenter.changeTheme(themes[it]) }
        unit.bind { presenter.changeWeightUnit(units[it]) }
        lead.bind { presenter.changeLeadTime(leads[it]) }
        reminders.setOnCheckedChangeListener { _, enabled ->
            if (!rendering) presenter.changeReminders(enabled, ReminderNotifications(requireContext()).allowed())
        }
        view.findViewById<View>(R.id.settings_retry).setOnClickListener {
            presenter.retry()
            app.refreshSettings()
        }
        view.findViewById<View>(R.id.notification_settings).setOnClickListener {
            startActivity(ReminderNotifications(requireContext()).settingsIntent())
        }
        renderState(presenter.state) { state ->
            rendering = true
            val enabled = !state.loading && !state.saving && state.settings != null
            listOf(language, theme, unit, lead, reminders).forEach { it.isEnabled = enabled }
            state.settings?.let {
                language.setSelection(languages.indexOf(it.language))
                theme.setSelection(themes.indexOf(it.theme))
                unit.setSelection(units.indexOf(it.weightUnit))
                lead.setSelection(leads.indexOf(it.reminderLeadTime))
                reminders.isChecked = it.remindersEnabled
            }
            rendering = false
            val messageId = when {
                state.loading -> R.string.settings_loading
                state.error == RepositoryError.CORRUPT_DATA -> R.string.error_local_data_corrupt
                state.error == RepositoryError.UNSUPPORTED_SCHEMA -> R.string.error_local_schema
                state.error != null -> R.string.settings_error
                state.reminderStatus != ReminderStatus.READY -> R.string.reminder_schedule_error
                state.saving -> R.string.profile_saving
                else -> null
            }
            view.findViewById<TextView>(R.id.settings_message).apply {
                isVisible = messageId != null
                text = messageId?.let(::getString)
            }
            view.findViewById<View>(R.id.settings_retry).isVisible = state.error != null || state.reminderStatus != ReminderStatus.READY
            view.findViewById<View>(R.id.reminder_permission_message).isVisible = state.permissionBlocked
            view.findViewById<View>(R.id.notification_settings).isVisible = state.permissionBlocked
            if (state.permissionRequest) whenResumed(view) {
                if (presenter.takePermissionRequest()) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        view.link(R.id.logout, Screen.REGISTRATION)
        view.findViewById<View>(R.id.terms).setOnClickListener {
            InformationDialog.show(this, R.string.terms, R.string.terms_unavailable)
        }
        view.findViewById<View>(R.id.privacy).setOnClickListener {
            InformationDialog.show(this, R.string.privacy, R.string.privacy_unavailable)
        }
        view.findViewById<View>(R.id.about).setOnClickListener {
            InformationDialog.show(this, R.string.about, R.string.about_description)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.refreshPermission(ReminderNotifications(requireContext()).allowed())
    }
}