package com.example.mobile.data.local

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PetPhotoReader(private val resolver: ContentResolver) {
    suspend fun read(uri: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val photo = uri.toUri()
            if (photo.scheme != ContentResolver.SCHEME_CONTENT) return@withContext null
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, photo)) { decoder, info, _ ->
                val scale = (256f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}