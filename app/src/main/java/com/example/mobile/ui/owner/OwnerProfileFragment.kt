package com.example.mobile.ui.owner

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile.presenter.owner.OwnerProfilePresenter
import com.example.mobile.domain.model.AccountMode
import com.example.mobile.R
import com.example.mobile.ui.common.PhoneMask
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.navigation.Screen
import com.example.mobile.ui.pets.PetAvatarLoader
import com.example.mobile.ui.pets.PetCardAdapter
import com.example.mobile.ui.pets.petStringResource
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.combine

class OwnerProfileFragment : ScreenFragment(R.layout.fragment_owner_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.link(R.id.edit_profile, Screen.EDIT_PROFILE)
        view.link(R.id.settings, Screen.SETTINGS)
        val presenter = retainedPresenter { scope -> OwnerProfilePresenter(repositories.owner, repositories.pets,
            repositories.authentication, repositories.localDataTransfer, scope) }
        val transfer = LocalDataTransferSection(view, presenter.transfer)
        renderState(presenter.transfer.state, transfer::render)
        val logout = view.findViewById<MaterialButton>(R.id.logout)
        val accountMessage = view.findViewById<TextView>(R.id.account_message)
        logout.setOnClickListener { presenter.logout() }
        renderState(presenter.account) { account ->
            view.findViewById<TextView>(R.id.account_mode).setText(if (account.mode == AccountMode.LOCAL) R.string.auth_local_mode else R.string.auth_server_mode)
            logout.setText(if (account.mode == AccountMode.LOCAL) R.string.auth_leave_local else R.string.auth_logout)
        }
        renderState(combine(presenter.accountAction, presenter.transfer.state) { action, transferState -> action to transferState.busy }) { (action, transferring) ->
            logout.isEnabled = !action.busy && !transferring
            accountMessage.isVisible = action.error != null
            accountMessage.text = action.error?.let { getString(it.stringResource()) }
        }
        val petList = view.findViewById<RecyclerView>(R.id.owner_pets_list)
        val petAdapter = PetCardAdapter(PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope), ownerCards = true) {
            navigator.open(Screen.PET_PROFILE, it)
        }
        petList.adapter = petAdapter
        val petsEmpty = view.findViewById<TextView>(R.id.owner_pets_empty)
        val name = view.findViewById<TextView>(R.id.owner_name)
        val email = view.findViewById<TextView>(R.id.owner_email)
        val phone = view.findViewById<TextView>(R.id.owner_phone)
        val message = view.findViewById<TextView>(R.id.profile_message)
        val retry = view.findViewById<MaterialButton>(R.id.profile_retry)
        val phoneMask = PhoneMask(getString(R.string.phone_hint))
        retry.setOnClickListener {
            if (presenter.state.value.error != null || presenter.state.value.petError != null) presenter.retry()
            else navigator.openRoot(Screen.REGISTRATION)
        }
        renderState(presenter.state) { state ->
            name.text = state.owner?.name.orEmpty()
            email.text = state.owner?.email.orEmpty()
            phone.text = phoneMask.format(state.owner?.phone.orEmpty())
            name.isVisible = state.owner != null
            email.isVisible = state.owner != null
            phone.isVisible = !state.owner?.phone.isNullOrBlank()
            view.findViewById<View>(R.id.edit_profile).isEnabled = state.owner != null && !state.loading && state.error == null
            petAdapter.submitList(state.pets)
            petList.isVisible = state.pets.isNotEmpty()
            petsEmpty.isVisible = state.owner != null && state.pets.isEmpty() && state.petError == null
            petsEmpty.setText(if (state.petsLoading) R.string.pet_loading else R.string.no_pets)
            val messageId = state.error?.stringResource() ?: state.petError?.petStringResource() ?: when {
                state.loading -> R.string.profile_loading
                state.owner == null -> R.string.account_required
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            retry.isVisible = !state.loading && (state.error != null || state.petError != null || state.owner == null)
            retry.setText(if (state.error != null || state.petError != null) R.string.retry else R.string.create_account)
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.owner_pets_list)?.adapter = null
        super.onDestroyView()
    }
}