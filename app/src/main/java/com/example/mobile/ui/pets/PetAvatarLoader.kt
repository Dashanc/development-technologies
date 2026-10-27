package com.example.mobile.ui.pets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.ImageViewCompat
import com.example.mobile.data.local.PetPhotoReader
import com.example.mobile.domain.model.PetSpecies
import com.example.mobile.R
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PetAvatarLoader(context: Context, private val scope: CoroutineScope) {
    private val context = context.applicationContext
    private val reader = PetPhotoReader(this.context.contentResolver)
    private val requests = WeakHashMap<ImageView, Pair<String?, PetSpecies>>()
    private val jobs = WeakHashMap<ImageView, Job>()
    private val paddings = WeakHashMap<ImageView, IntArray>()
    private val failed = mutableSetOf<String>()

    fun show(view: ImageView, uri: String?, species: PetSpecies) {
        val request = uri to species
        if (requests[view] == request) return
        jobs.remove(view)?.cancel()
        requests[view] = request
        val padding = paddings.getOrPut(view) { intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom) }
        fun placeholder() {
            view.setPadding(padding[0], padding[1], padding[2], padding[3])
            view.scaleType = ImageView.ScaleType.FIT_CENTER
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(context.getColor(R.color.mp_primary)))
            view.setImageResource(if (species == PetSpecies.CAT) R.drawable.ic_cat else R.drawable.ic_paw)
        }
        fun photo(bitmap: Bitmap) {
            view.setPadding(0, 0, 0, 0)
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.clipToOutline = true
            ImageViewCompat.setImageTintList(view, null)
            view.setImageBitmap(bitmap)
        }
        placeholder()
        if (uri == null) return
        cache.get(uri)?.let {
            photo(it)
            return
        }
        jobs[view] = scope.launch {
            val bitmap = reader.read(uri)
            if (requests[view] != request) return@launch
            if (bitmap != null) {
                cache.put(uri, bitmap)
                photo(bitmap)
            } else if (failed.add(uri)) {
                Toast.makeText(context, R.string.pet_photo_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun clear(view: ImageView) {
        jobs.remove(view)?.cancel()
        requests.remove(view)
    }

    companion object {
        private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.byteCount
        }
    }
}