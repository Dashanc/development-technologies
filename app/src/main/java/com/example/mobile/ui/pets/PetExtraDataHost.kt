package com.example.mobile.ui.pets

import com.example.mobile.domain.model.PetExtraData
import com.example.mobile.presenter.pets.PetFieldError
import kotlinx.coroutines.flow.Flow

data class PetExtraDataState(
    val extra: PetExtraData,
    val microchipError: PetFieldError? = null,
    val editable: Boolean = true,
)

interface PetExtraDataHost {
    val extraDataState: Flow<PetExtraDataState>
    fun changeExtra(extra: PetExtraData)
    fun confirmExtra(): Boolean
}