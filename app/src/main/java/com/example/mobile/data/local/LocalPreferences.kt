package com.example.mobile.data.local

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.io.IOException
import java.time.DateTimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

internal class LocalDataException(val error: RepositoryError) : RuntimeException()

internal class LocalPreferences(private val dataStore: DataStore<Preferences>) {
    fun <T> observe(read: (Preferences) -> T): Flow<RepositoryResult<T>> = dataStore.data
        .map<Preferences, RepositoryResult<T>> { RepositoryResult.Success(read(it)) }
        .catch { error ->
            val reason = when (error) {
                is LocalDataException -> error.error
                is CorruptionException, is ClassCastException -> RepositoryError.CORRUPT_DATA
                is IOException -> RepositoryError.READ_FAILED
                else -> throw error
            }
            emit(RepositoryResult.Failure(reason))
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun edit(update: (MutablePreferences) -> Unit): RepositoryResult<Unit> = try {
        dataStore.edit { update(it) }
        RepositoryResult.Success(Unit)
    } catch (error: LocalDataException) {
        RepositoryResult.Failure(error.error)
    } catch (_: CorruptionException) {
        RepositoryResult.Failure(RepositoryError.CORRUPT_DATA)
    } catch (_: IOException) {
        RepositoryResult.Failure(RepositoryError.WRITE_FAILED)
    } catch (_: ClassCastException) {
        RepositoryResult.Failure(RepositoryError.CORRUPT_DATA)
    } catch (_: SerializationException) {
        RepositoryResult.Failure(RepositoryError.INVALID_DATA)
    } catch (_: IllegalArgumentException) {
        RepositoryResult.Failure(RepositoryError.INVALID_DATA)
    } catch (_: DateTimeException) {
        RepositoryResult.Failure(RepositoryError.INVALID_DATA)
    }
}