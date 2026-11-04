package com.example.mobile.ui.pets

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.mobile.R
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.presenter.pets.EditPetDestination
import com.example.mobile.presenter.pets.EditPetPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.whenResumed
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class EditPetFragment : ScreenFragment(R.layout.fragment_add_pet), PetExtraDataHost {
    private lateinit var presenter: EditPetPresenter

    override val extraDataState get() = presenter.state.map {
        PetExtraDataState(it.input.extra, it.errors.microchip, it.editable)
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) presenter.selectPhoto(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val draft = savedInstanceState?.getBundle(PetEditArguments.DRAFT)?.let(PetEditArguments::input)
        val repositories = LocalRepositories.get(requireContext())
        presenter = retainedPresenter { scope ->
            EditPetPresenter(repositories.owner, repositories.pets, repositories.petPhotos, petId, scope, draft)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.pet_form_title).setText(R.string.edit_pet_title)
        val save = view.findViewById<MaterialButton>(R.id.save)
        val cancel = view.findViewById<View>(R.id.cancel_edit).apply {
            isVisible = true
            setOnClickListener { navigator.back() }
        }
        val extra = view.findViewById<MaterialButton>(R.id.extra).apply {
            setText(R.string.pet_edit_extra)
            setOnClickListener {
                showExtra()
            }
        }
        val fields = PetFormFields(view, { presenter.state.value.input }, presenter::changeInput)
        val photos = PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope)
        val photoSelect = view.findViewById<View>(R.id.photo_select)
        val photoLabel = view.findViewById<TextView>(R.id.choose_photo)
        val removePhoto = view.findViewById<View>(R.id.remove_photo)
        val message = view.findViewById<TextView>(R.id.pet_form_message)
        val savingBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = Unit
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, savingBack)
        view.findViewById<ImageView>(R.id.photo_badge).setImageResource(R.drawable.ic_edit)
        val choosePhoto = View.OnClickListener {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        photoSelect.setOnClickListener(choosePhoto)
        photoLabel.setOnClickListener(choosePhoto)
        removePhoto.setOnClickListener { presenter.removePhoto() }
        fields.showCalendar {
            val initial = presenter.calendarDate()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                fields.setBirth(PetText.inputDate(LocalDate.of(year, month + 1, day)))
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }
        save.setOnClickListener {
            presenter.submit()
            val errors = presenter.state.value.errors
            fields.focusError(errors)
            if (errors.microchip != null && errors.name == null && errors.birth == null && errors.sex == null) showExtra()
        }
        renderState(presenter.state) { state ->
            fields.render(state.input, state.errors, state.editable)
            photos.show(view.findViewById(R.id.pet_avatar), state.input.photoUri, state.input.species)
            val label = if (state.input.photoUri == null) R.string.add_photo else R.string.pet_replace_photo
            photoLabel.setText(label)
            photoSelect.contentDescription = getString(label)
            photoSelect.isEnabled = state.editable && !state.photoLoading
            photoLabel.isEnabled = photoSelect.isEnabled
            removePhoto.isVisible = state.input.photoUri != null
            removePhoto.isEnabled = photoSelect.isEnabled
            extra.isEnabled = state.editable
            save.isEnabled = !state.loading && !state.saving && !state.saved && !state.photoLoading && state.destination == null
            save.setText(when {
                state.loading -> R.string.pet_loading
                state.saving -> R.string.profile_saving
                state.photoLoading -> R.string.pet_photo_loading
                state.loadFailed -> R.string.retry
                state.ownerMissing -> R.string.create_account
                state.petMissing -> R.string.pet_back_to_list
                else -> R.string.save_changes
            })
            cancel.isEnabled = !state.saving
            view.findViewById<View>(R.id.back).isEnabled = !state.saving
            savingBack.isEnabled = state.saving
            val messageId = state.error?.petStringResource() ?: when {
                state.loading -> R.string.pet_loading
                state.ownerMissing -> R.string.local_profile_missing
                state.petMissing -> R.string.pet_not_found
                state.photoFailed -> R.string.error_pet_photo
                state.photoLoading -> R.string.pet_photo_loading
                state.errors.microchip != null -> R.string.error_pet_microchip
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            if (state.destination != null) whenResumed(view) {
                when (presenter.takeDestination()) {
                    EditPetDestination.SAVED -> {
                        Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
                        navigator.back()
                    }
                    EditPetDestination.REGISTRATION -> navigator.openRoot(Screen.REGISTRATION)
                    EditPetDestination.PET_LIST -> navigator.openRoot(Screen.PET_LIST)
                    null -> Unit
                }
            }
        }
    }

    override fun changeExtra(extra: PetExtraData) = presenter.changeExtra(extra)

    override fun confirmExtra() = presenter.validateExtra()

    override fun onSaveInstanceState(outState: Bundle) {
        presenter.currentDraft()?.let {
            outState.putBundle(PetEditArguments.DRAFT, PetEditArguments.fromInput(it))
        }
        super.onSaveInstanceState(outState)
    }

    private fun showExtra() {
        if (!childFragmentManager.isStateSaved && childFragmentManager.findFragmentByTag(PetExtraDataSheet.TAG) == null) {
            PetExtraDataSheet().showNow(childFragmentManager, PetExtraDataSheet.TAG)
        }
    }
}