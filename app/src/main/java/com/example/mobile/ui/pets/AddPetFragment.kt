package com.example.mobile.ui.pets

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.mobile.data.local.LocalRepositories
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.presenter.pets.AddPetPresenter
import com.example.mobile.R
import com.example.mobile.ui.common.renderState
import com.example.mobile.ui.common.retainedPresenter
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.whenResumed
import com.example.mobile.ui.navigation.Screen
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class AddPetFragment : ScreenFragment(R.layout.fragment_add_pet), PetExtraDataHost {
    internal lateinit var presenter: AddPetPresenter
        private set

    override val extraDataState get() = presenter.state.map {
        PetExtraDataState(it.input.extra, it.errors.microchip, it.editable)
    }

    override fun changeExtra(extra: PetExtraData) = presenter.changeExtra(extra)

    override fun confirmExtra() = presenter.validateExtra()

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) presenter.selectPhoto(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repositories = LocalRepositories.get(requireContext())
        presenter = retainedPresenter { scope ->
            AddPetPresenter(repositories.owner, repositories.pets, repositories.petPhotos, scope)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fields = PetFormFields(view, { presenter.state.value.input }, presenter::changeInput)
        val photos = PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope)
        val avatar = view.findViewById<ImageView>(R.id.pet_avatar)
        val photoSelect = view.findViewById<View>(R.id.photo_select)
        val photoLabel = view.findViewById<View>(R.id.choose_photo)
        val removePhoto = view.findViewById<View>(R.id.remove_photo)
        val extra = view.findViewById<View>(R.id.extra)
        val save = view.findViewById<MaterialButton>(R.id.save)
        val message = view.findViewById<TextView>(R.id.pet_form_message)
        val choosePhoto = View.OnClickListener {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        photoSelect.setOnClickListener(choosePhoto)
        photoLabel.setOnClickListener(choosePhoto)
        removePhoto.setOnClickListener { presenter.removePhoto() }
        extra.setOnClickListener { showExtra() }
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
            photos.show(avatar, state.input.photoUri, state.input.species)
            photoSelect.isEnabled = state.editable && !state.photoLoading
            photoLabel.isEnabled = photoSelect.isEnabled
            removePhoto.isVisible = state.input.photoUri != null
            removePhoto.isEnabled = photoSelect.isEnabled
            extra.isEnabled = state.editable
            save.isEnabled = !state.loading && !state.saving && !state.photoLoading
            save.setText(when {
                state.loading -> R.string.pet_loading
                state.saving -> R.string.profile_saving
                state.photoLoading -> R.string.pet_photo_loading
                state.loadFailed -> R.string.retry
                state.ownerMissing -> R.string.create_account
                else -> R.string.save_pet
            })
            view.findViewById<View>(R.id.back).isEnabled = !state.saving
            val messageId = state.error?.petStringResource() ?: when {
                state.ownerMissing -> R.string.local_profile_missing
                state.photoFailed -> R.string.error_pet_photo
                state.photoLoading -> R.string.pet_photo_loading
                state.errors.microchip != null -> R.string.error_pet_microchip
                else -> null
            }
            message.isVisible = messageId != null
            message.text = messageId?.let(::getString)
            if (state.savedPetId != null || state.openRegistration) whenResumed(view) {
                val savedId = presenter.takeSavedPet()
                if (savedId != null) navigator.replace(Screen.PET_PROFILE, savedId)
                else if (presenter.takeRegistration()) navigator.openRoot(Screen.REGISTRATION)
            }
        }
    }

    private fun showExtra() {
        if (!childFragmentManager.isStateSaved && childFragmentManager.findFragmentByTag(PetExtraDataSheet.TAG) == null) {
            PetExtraDataSheet().showNow(childFragmentManager, PetExtraDataSheet.TAG)
        }
    }
}