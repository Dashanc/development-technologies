package com.example.mobile.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.LocaleList
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mobile.MainActivity
import com.example.mobile.R
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.presenter.health.HealthEventType
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class ReminderNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun allowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
        manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    fun configure(language: AppLanguage) {
        val text = localized(language)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, text.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = text.getString(R.string.reminder_channel_description)
        })
    }

    fun show(plan: ReminderPlan, language: AppLanguage) {
        if (!allowed()) return
        val text = localized(language)
        val event = plan.event
        val kind = text.getString(if (event.type == HealthEventType.VACCINATION) R.string.vaccination else R.string.vet_record)
        val date = event.date.format(DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.forLanguageTag(language.languageTag)))
        val body = text.getString(R.string.reminder_body, event.petName, kind, date, event.description)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.getString(R.string.reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        manager.notify(plan.identity, 0, notification)
    }

    fun cancel(identity: String) = manager.cancel(identity, 0)

    fun settingsIntent(): Intent = Intent(
        if (manager.areNotificationsEnabled()) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS,
    ).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
    }

    private fun localized(language: AppLanguage): Context = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
        setLocales(LocaleList.forLanguageTags(language.languageTag))
    })

    companion object {
        const val CHANNEL_ID = "pet_reminders_v1"
    }
}