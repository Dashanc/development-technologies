package com.example.mobile.domain.repository

import com.example.mobile.domain.model.AccountSession
import com.example.mobile.domain.model.Owner
import kotlinx.coroutines.flow.StateFlow

interface AuthenticationRepository {
    val session: StateFlow<AccountSession>
    suspend fun restore(): RepositoryResult<Unit>
    suspend fun register(name: String, email: String, phone: String, password: CharArray): RepositoryResult<Owner>
    suspend fun login(email: String, password: CharArray): RepositoryResult<Owner>
    suspend fun logout(): RepositoryResult<Unit>
    suspend fun openLocalProfile(): RepositoryResult<Unit>
}