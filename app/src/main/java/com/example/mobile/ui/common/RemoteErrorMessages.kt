package com.example.mobile.ui.common

import com.example.mobile.R
import com.example.mobile.domain.repository.RepositoryError

internal fun RepositoryError.remoteStringResource(): Int = when (this) {
    RepositoryError.NETWORK_UNAVAILABLE -> R.string.remote_error_network
    RepositoryError.REQUEST_TIMEOUT -> R.string.remote_error_timeout
    RepositoryError.AUTHENTICATION_REQUIRED -> R.string.remote_error_authentication
    RepositoryError.ACCESS_DENIED -> R.string.remote_error_access
    RepositoryError.CONFLICT -> R.string.remote_error_conflict
    RepositoryError.VERSION_CONFLICT -> R.string.remote_error_version
    RepositoryError.RATE_LIMITED -> R.string.remote_error_rate
    RepositoryError.SERVER_UNAVAILABLE -> R.string.remote_error_server
    RepositoryError.NOT_CONFIGURED -> R.string.remote_error_configuration
    else -> R.string.remote_error_response
}