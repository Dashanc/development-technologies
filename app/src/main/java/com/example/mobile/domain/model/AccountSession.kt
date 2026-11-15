package com.example.mobile.domain.model

import com.example.mobile.domain.repository.RepositoryError

enum class AccountMode { RESTORING, SIGNED_OUT, SERVER, LOCAL }

data class AccountSession(val mode: AccountMode = AccountMode.RESTORING, val ownerId: String? = null, val restoreError: RepositoryError? = null)