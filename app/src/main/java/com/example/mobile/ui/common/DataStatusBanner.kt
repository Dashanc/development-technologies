package com.example.mobile.ui.common

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mobile.R
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.presenter.DataMessage
import com.example.mobile.presenter.DataStatusPresenter
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

internal class DataStatusBanner(view: View, owner: LifecycleOwner, presenter: DataStatusPresenter) {
    init {
        val banner = view.findViewById<View>(R.id.data_status_banner)
        val text = view.findViewById<TextView>(R.id.data_status_text)
        val retry = view.findViewById<View>(R.id.data_status_retry)
        retry.setOnClickListener { presenter.retry() }
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                presenter.state.collect { state ->
                    banner.isVisible = state.message != null
                    retry.isVisible = state.canRetry
                    val messageId = when (state.message) {
                        DataMessage.UPDATING -> R.string.cache_updating
                        DataMessage.CACHED -> R.string.cache_showing_saved
                        DataMessage.UNAVAILABLE -> when (state.error) {
                            RepositoryError.READ_FAILED, RepositoryError.WRITE_FAILED,
                            RepositoryError.CORRUPT_DATA, RepositoryError.UNSUPPORTED_SCHEMA -> R.string.cache_error_storage
                            else -> state.error?.remoteStringResource() ?: R.string.cache_no_data
                        }
                        null -> null
                    }
                    val message = messageId?.let { view.context.getString(it) }.orEmpty()
                    text.text = if (state.message == DataMessage.CACHED && state.updatedAt != null) {
                        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, view.resources.configuration.locales[0])
                        view.context.getString(R.string.cache_saved_at, message, format.format(Date(state.updatedAt)))
                    } else message
                }
            }
        }
    }
}