package com.example.mobile.ui.pets

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.mobile.R
import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.presenter.pets.PetInput
import com.example.mobile.presenter.pets.PetInputErrors
import com.example.mobile.ui.common.ScreenFragment
import com.example.mobile.ui.common.renderState
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class EditPetFragment : ScreenFragment(R.layout.fragment_add_pet), PetExtraDataHost {
    private val input = MutableStateFlow(PetInput())

    override val extraDataState get() = input.map { PetExtraDataState(it.extra) }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) input.value = input.value.copy(photoUri = uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val draft = savedInstanceState?.getBundle(PetEditArguments.DRAFT)
            ?: arguments?.getBundle(PetEditArguments.DRAFT)
        if (draft != null) input.value = PetEditArguments.input(draft)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.pet_form_title).setText(R.string.edit_pet_title)
        view.findViewById<MaterialButton>(R.id.save).apply {
            setText(R.string.save_changes)
            isEnabled = false
        }
        view.findViewById<View>(R.id.cancel_edit).apply {
            isVisible = true
            setOnClickListener { navigator.back() }
        }
        view.findViewById<MaterialButton>(R.id.extra).apply {
            setText(R.string.pet_edit_extra)
            setOnClickListener {
                if (!childFragmentManager.isStateSaved && childFragmentManager.findFragmentByTag(PetExtraDataSheet.TAG) == null) {
                    PetExtraDataSheet().showNow(childFragmentManager, PetExtraDataSheet.TAG)
                }
            }
        }
        val fields = PetFormFields(view, { input.value }, { input.value = it })
        val photos = PetAvatarLoader(requireContext(), viewLifecycleOwner.lifecycleScope)
        val photoSelect = view.findViewById<View>(R.id.photo_select)
        val photoLabel = view.findViewById<TextView>(R.id.choose_photo)
        val removePhoto = view.findViewById<View>(R.id.remove_photo)
        view.findViewById<ImageView>(R.id.photo_badge).setImageResource(R.drawable.ic_edit)
        val choosePhoto = View.OnClickListener {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        photoSelect.setOnClickListener(choosePhoto)
        photoLabel.setOnClickListener(choosePhoto)
        removePhoto.setOnClickListener { input.value = input.value.copy(photoUri = null) }
        fields.showCalendar {
            val initial = runCatching {
                LocalDate.parse(input.value.birth, DateTimeFormatter.ofPattern("dd.MM.uuuu"))
            }.getOrDefault(LocalDate.now())
            DatePickerDialog(requireContext(), { _, year, month, day ->
                fields.setBirth(PetText.inputDate(LocalDate.of(year, month + 1, day)))
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }
        renderState(input) { draft ->
            fields.render(draft, PetInputErrors(), enabled = true)
            photos.show(view.findViewById(R.id.pet_avatar), draft.photoUri, draft.species)
            val label = if (draft.photoUri == null) R.string.add_photo else R.string.pet_replace_photo
            photoLabel.setText(label)
            photoSelect.contentDescription = getString(label)
            removePhoto.isVisible = draft.photoUri != null
        }
    }

    override fun changeExtra(extra: PetExtraData) {
        input.value = input.value.copy(extra = extra)
    }

    override fun confirmExtra() = true

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle(PetEditArguments.DRAFT, PetEditArguments.fromInput(input.value))
        super.onSaveInstanceState(outState)
    }
}