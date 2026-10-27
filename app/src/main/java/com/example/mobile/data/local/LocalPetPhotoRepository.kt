package com.example.mobile.data.local

import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.example.mobile.domain.repository.PetPhotoRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LocalPetPhotoRepository(private val resolver: ContentResolver) : PetPhotoRepository {
    override suspend fun retain(uri: String): RepositoryResult<String> = withContext(Dispatchers.IO) {
        try {
            val photo = uri.toUri()
            if (photo.scheme != ContentResolver.SCHEME_CONTENT) return@withContext RepositoryResult.Failure(RepositoryError.INVALID_DATA)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(photo)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext RepositoryResult.Failure(RepositoryError.INVALID_DATA)
            }
            resolver.takePersistableUriPermission(photo, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            RepositoryResult.Success(photo.toString())
        } catch (_: IOException) {
            RepositoryResult.Failure(RepositoryError.READ_FAILED)
        } catch (_: SecurityException) {
            RepositoryResult.Failure(RepositoryError.READ_FAILED)
        } catch (_: IllegalArgumentException) {
            RepositoryResult.Failure(RepositoryError.INVALID_DATA)
        }
    }
}