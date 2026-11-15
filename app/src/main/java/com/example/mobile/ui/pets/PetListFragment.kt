package com.example.mobile.ui.pets

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.presenter.pets.PetListPresenter
import com.example.mobile.R
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton

class PetListFragment : ScreenFragment(R.layout.fragment_pet_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        val presenter = retainedPresenter { scope ->
            PetListPresenter(repositories.owner, repositories.pets, repositories.medicalRecords, repositories.settings, scope)
        }
        val list = view.findViewById<RecyclerView>(R.id.pets_list)
        val photos = PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope)
        val adapter = PetCardAdapter(photos) { navigator.open(Screen.PET_PROFILE, it) }
        list.adapter = adapter
        val message = view.findViewById<TextView>(R.id.pets_empty)
        val retry = view.findViewById<View>(R.id.pets_retry)
        val add = view.findViewById<MaterialButton>(R.id.add_pet)
        val addTop = view.findViewById<View>(R.id.add_top)
        val addAction = View.OnClickListener {
            navigator.open(if (presenter.state.value.ownerMissing) Screen.REGISTRATION else Screen.ADD_PET)
        }
        add.setOnClickListener(addAction)
        addTop.setOnClickListener(addAction)
        retry.setOnClickListener { presenter.retry() }
        renderState(presenter.state) { state ->
            adapter.weightUnit = state.weightUnit
            adapter.submitList(state.pets)
            list.isVisible = state.pets.isNotEmpty()
            val messageId = state.error?.petStringResource() ?: when {
                state.loading -> R.string.pet_loading
                state.ownerMissing -> R.string.local_profile_missing
                state.pets.isEmpty() -> R.string.no_pets
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            retry.isVisible = state.error != null
            add.isEnabled = !state.loading && state.error == null
            addTop.isEnabled = add.isEnabled
            add.setText(if (state.ownerMissing) R.string.create_account else R.string.add_pet)
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.pets_list)?.adapter = null
        super.onDestroyView()
    }
}