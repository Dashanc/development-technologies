package com.example.mobile.ui.health

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.health.HealthOverviewPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.navigation.Screen
import com.example.mobile.ui.pets.PetAvatarLoader
import com.google.android.material.button.MaterialButton

class HealthOverviewFragment : ScreenFragment(R.layout.fragment_health_overview) {
    private lateinit var presenter: HealthOverviewPresenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.all_records, Screen.HEALTH_HISTORY)
        val repositories = LocalRepositories.get(requireContext())
        presenter = retainedPresenter { scope -> HealthOverviewPresenter(repositories.owner, repositories.pets, repositories.medicalRecords, scope, repositories.settings) }
        val photos = PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope)
        val pets = HealthPetAdapter(photos) { navigator.open(Screen.PET_PROFILE, it) }
        val events = UpcomingEventAdapter { navigator.open(Screen.HEALTH_HISTORY, it) }
        view.findViewById<RecyclerView>(R.id.health_pets_list).adapter = pets
        view.findViewById<RecyclerView>(R.id.upcoming_events_list).adapter = events
        val message = view.findViewById<TextView>(R.id.health_message)
        val action = view.findViewById<MaterialButton>(R.id.health_action)
        action.setOnClickListener {
            when {
                presenter.state.value.error != null -> presenter.retry()
                presenter.state.value.ownerMissing -> navigator.openRoot(Screen.REGISTRATION)
                else -> navigator.open(Screen.ADD_PET)
            }
        }
        renderState(presenter.state) { state ->
            pets.weightUnit = state.weightUnit
            pets.submitList(state.pets)
            events.submitList(state.events)
            val ready = !state.loading && state.error == null && !state.ownerMissing
            val messageId = state.error?.medicalStringResource() ?: when {
                state.loading -> R.string.medical_loading
                state.ownerMissing -> R.string.local_profile_missing
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            action.isVisible = !state.loading && (state.error != null || state.ownerMissing || state.pets.isEmpty())
            action.setText(when {
                state.error != null -> R.string.retry
                state.ownerMissing -> R.string.create_account
                else -> R.string.add_pet
            })
            view.findViewById<View>(R.id.health_summary_card).isVisible = ready && state.pets.isNotEmpty()
            view.findViewById<TextView>(R.id.health_summary).text = getString(
                R.string.pet_detail_join,
                resources.getQuantityString(R.plurals.health_pet_count, state.pets.size, state.pets.size),
                resources.getQuantityString(R.plurals.health_event_count, state.events.size, state.events.size),
            )
            view.findViewById<View>(R.id.upcoming_heading).isVisible = ready
            view.findViewById<View>(R.id.upcoming_empty).isVisible = ready && state.events.isEmpty()
            view.findViewById<View>(R.id.health_pets_heading).isVisible = ready
            view.findViewById<View>(R.id.health_pets_empty).isVisible = ready && state.pets.isEmpty()
            view.findViewById<View>(R.id.all_records).isEnabled = ready && state.pets.isNotEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.refresh()
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.health_pets_list)?.adapter = null
        view?.findViewById<RecyclerView>(R.id.upcoming_events_list)?.adapter = null
        super.onDestroyView()
    }
}