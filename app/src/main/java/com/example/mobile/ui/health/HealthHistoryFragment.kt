package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.health.HealthHistoryPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton

class HealthHistoryFragment : ScreenFragment(R.layout.fragment_health_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope -> HealthHistoryPresenter(repositories.owner, repositories.pets, repositories.medicalRecords, petId, scope) }
        val adapter = MedicalHistoryAdapter()
        val list = view.findViewById<RecyclerView>(R.id.health_records)
        list.adapter = adapter
        val message = view.findViewById<TextView>(R.id.records_empty)
        val retry = view.findViewById<View>(R.id.medical_history_retry)
        val add = view.findViewById<MaterialButton>(R.id.add_record)
        retry.setOnClickListener { presenter.retry() }
        add.setOnClickListener {
            val state = presenter.state.value
            when {
                state.ownerMissing -> navigator.openRoot(Screen.REGISTRATION)
                state.noPets -> navigator.open(Screen.ADD_PET)
                state.petMissing -> navigator.openRoot(Screen.PET_LIST)
                else -> navigator.open(Screen.WEIGHT_ENTRY, petId)
            }
        }
        renderState(presenter.state) { state ->
            adapter.submitList(state.items)
            view.findViewById<TextView>(R.id.pet_name).text = state.petName ?: getString(R.string.medical_all_pets)
            view.findViewById<TextView>(R.id.history_summary).text = resources.getQuantityString(R.plurals.medical_record_count, state.items.size, state.items.size)
            val messageId = state.error?.medicalStringResource() ?: when {
                state.loading -> R.string.medical_loading
                state.ownerMissing -> R.string.local_profile_missing
                state.petMissing -> R.string.pet_not_found
                state.items.isEmpty() -> R.string.no_health_records
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            retry.isVisible = state.error != null
            add.isEnabled = !state.loading && state.error == null
            add.setText(when {
                state.ownerMissing -> R.string.create_account
                state.noPets -> R.string.add_pet
                state.petMissing -> R.string.pet_back_to_list
                else -> R.string.add_record
            })
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.health_records)?.adapter = null
        super.onDestroyView()
    }
}