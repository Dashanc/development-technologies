package com.example.mobile.ui.owner

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.mobile.R
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.presenter.owner.LocalDataTransferPresenter
import com.example.mobile.presenter.owner.LocalDataTransferState
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

internal class LocalDataTransferSection(view: View, private val presenter: LocalDataTransferPresenter) {
    private val section = view.findViewById<View>(R.id.local_data_transfer)
    private val summary = view.findViewById<TextView>(R.id.transfer_summary)
    private val status = view.findViewById<TextView>(R.id.transfer_status)
    private val includeOwner = view.findViewById<MaterialCheckBox>(R.id.transfer_include_owner)
    private val consent = view.findViewById<MaterialCheckBox>(R.id.transfer_consent)
    private val action = view.findViewById<MaterialButton>(R.id.transfer_action)
    private val review = view.findViewById<MaterialButton>(R.id.transfer_review)

    init {
        includeOwner.setOnCheckedChangeListener { _, checked -> presenter.setIncludeOwner(checked) }
        consent.setOnCheckedChangeListener { _, checked -> presenter.setConsent(checked) }
        action.setOnClickListener { if (presenter.state.value.preview == null) presenter.retry() else presenter.transfer() }
        review.setOnClickListener { presenter.retry() }
    }

    fun render(state: LocalDataTransferState) {
        section.isVisible = state.visible
        val preview = state.preview
        val editable = preview != null && !preview.completed && !state.busy && !state.loading
        summary.text = when {
            preview == null -> section.context.getString(R.string.transfer_description)
            preview.completed -> section.context.getString(R.string.transfer_done_summary, preview.petCount, preview.recordCount)
            else -> section.context.getString(R.string.transfer_preview,
                preview.localOwner.name, preview.localOwner.email, preview.localOwner.phone,
                preview.serverOwner.name, preview.serverOwner.email, preview.serverOwner.phone,
                preview.petCount, preview.recordCount)
        }
        includeOwner.isVisible = preview != null && !preview.completed
        includeOwner.isEnabled = editable && preview?.started == false
        includeOwner.isChecked = state.includeOwner
        consent.isVisible = preview != null && !preview.completed
        consent.isEnabled = editable
        consent.isChecked = state.consent
        val message = when {
            state.busy -> section.context.getString(R.string.transfer_progress, state.progress.transferred, state.progress.total)
            state.error == RepositoryError.TRANSFER_RETRY_EXPIRED -> section.context.getString(R.string.transfer_retry_expired)
            state.error == RepositoryError.VERSION_CONFLICT -> section.context.getString(R.string.transfer_profile_conflict)
            state.error == RepositoryError.INVALID_DATA -> section.context.getString(R.string.transfer_invalid_data)
            state.error != null -> section.context.getString(state.error.stringResource())
            state.loading -> section.context.getString(R.string.transfer_loading)
            preview?.completed == true -> section.context.getString(R.string.transfer_completed)
            preview?.started == true -> section.context.getString(R.string.transfer_interrupted, state.progress.transferred, state.progress.total)
            else -> null
        }
        status.isVisible = message != null
        status.text = listOfNotNull(message, state.progress.item, state.fieldErrors.takeIf { it.isNotEmpty() }?.joinToString("\n")).joinToString("\n")
        action.isVisible = preview?.completed != true
        action.isEnabled = !state.busy && !state.loading && (preview == null || state.consent) && state.error != RepositoryError.TRANSFER_RETRY_EXPIRED
        action.setText(when {
            preview == null -> R.string.retry
            preview.started -> R.string.transfer_resume
            else -> R.string.transfer_confirm
        })
        review.isVisible = editable && state.error != RepositoryError.TRANSFER_RETRY_EXPIRED && (preview?.started == false || state.error != null)
    }
}