package com.example.mobile.ui.pets

import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.presenter.pets.PetFieldError
import com.example.mobile.R

internal fun RepositoryError.petStringResource(): Int = when (this) {
    RepositoryError.READ_FAILED -> R.string.error_pet_read
    RepositoryError.WRITE_FAILED -> R.string.error_pet_write
    RepositoryError.CORRUPT_DATA -> R.string.error_local_data_corrupt
    RepositoryError.UNSUPPORTED_SCHEMA -> R.string.error_local_schema
    RepositoryError.INVALID_REFERENCE -> R.string.local_profile_missing
    RepositoryError.INVALID_DATA, RepositoryError.IDENTITY_CONFLICT -> R.string.error_pet_data
}

internal fun PetFieldError.stringResource(): Int = when (this) {
    PetFieldError.NAME_REQUIRED -> R.string.error_pet_name
    PetFieldError.BIRTH_REQUIRED -> R.string.error_pet_birth_required
    PetFieldError.BIRTH_INVALID -> R.string.error_pet_birth_invalid
    PetFieldError.BIRTH_FUTURE -> R.string.error_pet_birth_future
    PetFieldError.SEX_REQUIRED -> R.string.error_pet_sex
    PetFieldError.MICROCHIP_INVALID -> R.string.error_pet_microchip
}