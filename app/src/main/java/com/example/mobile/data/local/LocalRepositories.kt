package com.example.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.mobile.domain.repository.MedicalRecordRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.PetPhotoRepository
import com.example.mobile.domain.repository.PetRepository
import com.example.mobile.domain.repository.SettingsRepository

private val Context.passportDataStore by preferencesDataStore(name = "mur_passport")

class LocalRepositories private constructor(context: Context) {
    private val preferences = LocalPreferences(context.applicationContext.passportDataStore)
    private val storage = PassportStorage(preferences)

    val owner: OwnerRepository = LocalOwnerRepository(storage)
    val pets: PetRepository = LocalPetRepository(storage)
    val petPhotos: PetPhotoRepository = LocalPetPhotoRepository(context.applicationContext.contentResolver)
    val medicalRecords: MedicalRecordRepository = LocalMedicalRecordRepository(storage)
    val settings: SettingsRepository = LocalSettingsRepository(preferences)
    internal val reminderStorage = LocalReminderStorage(preferences)

    companion object {
        @Volatile
        private var instance: LocalRepositories? = null

        fun get(context: Context): LocalRepositories = instance ?: synchronized(this) {
            instance ?: LocalRepositories(context.applicationContext).also { instance = it }
        }
    }
}