package com.example.mobile.ui.owner

import com.example.mobile.R
import com.example.mobile.domain.repository.RepositoryError

internal fun RepositoryError.stringResource(): Int = when (this) {
    RepositoryError.READ_FAILED -> R.string.error_profile_read
    RepositoryError.WRITE_FAILED -> R.string.error_profile_write
    RepositoryError.CORRUPT_DATA -> R.string.error_local_data_corrupt
    RepositoryError.UNSUPPORTED_SCHEMA -> R.string.error_local_schema
    RepositoryError.IDENTITY_CONFLICT -> R.string.error_profile_exists
    RepositoryError.INVALID_DATA, RepositoryError.INVALID_REFERENCE -> R.string.error_profile_data
}