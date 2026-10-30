package com.example.mobile.ui.pets

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.pets.PetProfilePresenter
import com.example.mobile.R
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton
import com.example.mobile.ui.health.MedicalText

class PetProfileFragment : ScreenFragment(R.layout.fragment_pet_profile) {
    private lateinit var presenter: PetProfilePresenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.log_weight, Screen.WEIGHT_ENTRY)
        view.link(R.id.add_event, Screen.VACCINATION_ENTRY)
        view.link(R.id.history, Screen.HEALTH_HISTORY)
        view.link(R.id.upcoming_event, Screen.HEALTH_HISTORY)
        val repositories = LocalRepositories.get(requireContext())
        presenter = retainedPresenter { scope -> PetProfilePresenter(repositories.owner, repositories.pets, repositories.medicalRecords, petId, scope) }
        val photos = PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope)
        val text = PetText(requireContext())
        val medicalText = MedicalText(requireContext())
        val message = view.findViewById<TextView>(R.id.pet_profile_message)
        val retry = view.findViewById<MaterialButton>(R.id.pet_profile_retry)
        retry.setOnClickListener {
            when {
                presenter.state.value.error != null -> presenter.retry()
                presenter.state.value.ownerMissing -> navigator.openRoot(Screen.REGISTRATION)
                else -> navigator.openRoot(Screen.PET_LIST)
            }
        }
        renderState(presenter.state) { state ->
            val card = state.card
            view.findViewById<View>(R.id.pet_profile_content).isVisible = card != null
            val messageId = state.error?.petStringResource() ?: when {
                state.loading -> R.string.pet_loading
                state.ownerMissing -> R.string.local_profile_missing
                card == null -> R.string.pet_not_found
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            retry.isVisible = !state.loading && (card == null || state.error != null)
            retry.setText(when {
                state.error != null -> R.string.retry
                state.ownerMissing -> R.string.create_account
                else -> R.string.pet_back_to_list
            })
            if (card != null) {
                view.findViewById<TextView>(R.id.pet_name).text = card.pet.name
                view.findViewById<TextView>(R.id.pet_breed).text = text.breed(card.pet)
                view.findViewById<TextView>(R.id.age).text = text.age(card.age)
                view.findViewById<TextView>(R.id.current_weight).text = state.lastWeightKg?.let(medicalText::weight) ?: getString(R.string.pet_no_value)
                view.findViewById<TextView>(R.id.history).text = state.recordCount?.let {
                    getString(R.string.medical_history_count, resources.getQuantityString(R.plurals.medical_record_count, it, it))
                } ?: getString(R.string.all_history)
                view.findViewById<TextView>(R.id.pet_extra_summary).text = text.extra(card.pet)
                val event = state.upcomingEvents.firstOrNull()
                val eventsReady = state.recordCount != null && state.error == null
                view.findViewById<View>(R.id.upcoming_heading).isVisible = eventsReady
                view.findViewById<View>(R.id.pet_upcoming_empty).isVisible = eventsReady && event == null
                view.findViewById<View>(R.id.upcoming_event).isVisible = eventsReady && event != null
                view.findViewById<TextView>(R.id.event_title).text = event?.let(medicalText::eventTitle)
                view.findViewById<TextView>(R.id.event_date).text = event?.let(medicalText::eventDescription)
                photos.show(view.findViewById<ImageView>(R.id.pet_avatar), card.pet.photoUri, card.pet.species)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.refresh()
    }
}