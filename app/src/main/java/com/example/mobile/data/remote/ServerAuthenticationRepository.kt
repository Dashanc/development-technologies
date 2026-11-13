package com.example.mobile.data.remote

import com.example.mobile.data.local.SecureSessionStorage
import com.example.mobile.data.local.AccountModeStorage
import com.example.mobile.data.local.StoredSession
import com.example.mobile.domain.model.AccountMode
import com.example.mobile.domain.model.AccountSession
import com.example.mobile.domain.model.Owner
import com.example.mobile.domain.repository.AuthenticationRepository
import com.example.mobile.domain.repository.OwnerRepository
import com.example.mobile.domain.repository.RepositoryError
import com.example.mobile.domain.repository.RepositoryResult
import com.example.mobile.data.remote.dto.AuthenticationDto
import com.example.mobile.data.remote.dto.LoginDto
import com.example.mobile.data.remote.dto.RegisterDto
import com.example.mobile.data.remote.dto.SessionDto
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

internal class ServerAuthenticationRepository(
    private val api: ApiClient,
    private val sessions: MutableStateFlow<RemoteSession?>,
    private val storage: SecureSessionStorage,
    private val localOwner: OwnerRepository,
    private val modeStorage: AccountModeStorage,
    private val clearAccount: suspend () -> Unit,
) : AuthenticationRepository {
    private val mutableSession = MutableStateFlow(AccountSession())
    override val session = mutableSession.asStateFlow()
    private val mutex = Mutex()

    override suspend fun restore(): RepositoryResult<Unit> = mutex.withLock {
        if (session.value.mode != AccountMode.RESTORING && session.value.restoreError == null) return@withLock RepositoryResult.Success(Unit)
        mutableSession.value = AccountSession()
        val result = remoteResult {
            if (value(modeStorage.read())) {
                val owner = value(localOwner.observeOwner().first())
                if (owner != null) {
                    mutableSession.value = AccountSession(AccountMode.LOCAL, owner.id)
                    return@remoteResult
                }
                value(modeStorage.setLocal(false))
            }
            val saved = secure { storage.read() }
            if (saved == null) {
                clearAccount()
                mutableSession.value = AccountSession(AccountMode.SIGNED_OUT)
                return@remoteResult
            }
            if (saved.server != api.server || !Instant.parse(saved.expiresAt).isAfter(Instant.now())) {
                clear()
                return@remoteResult
            }
            val active = RemoteSession(saved.token, saved.ownerId)
            sessions.value = active
            try {
                val response = api.request(active, "GET", listOf("auth", "session"))
                val detail = api.decode(response, SessionDto.serializer())
                if (detail.owner.id != active.ownerId || !Instant.parse(detail.expiresAt).isAfter(Instant.now())) {
                    throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
                }
                secure { storage.write(StoredSession(saved.token, active.ownerId, detail.expiresAt, api.server)) }
                mutableSession.value = AccountSession(AccountMode.SERVER, active.ownerId)
            } catch (error: RemoteFailure) {
                if (error.error == RepositoryError.AUTHENTICATION_REQUIRED) {
                    clear()
                    return@remoteResult
                }
                throw error
            }
        }
        if (result is RepositoryResult.Failure) {
            sessions.value = null
            mutableSession.value = AccountSession(AccountMode.SIGNED_OUT, restoreError = result.error)
        }
        result
    }

    override suspend fun register(name: String, email: String, phone: String, password: CharArray) = authenticate(password) {
        api.json.encodeToJsonElement(RegisterDto(name, email, phone.ifBlank { null }, password.concatToString())) to "register"
    }

    override suspend fun login(email: String, password: CharArray) = authenticate(password) {
        api.json.encodeToJsonElement(LoginDto(email, password.concatToString())) to "login"
    }

    private suspend fun authenticate(password: CharArray, request: () -> Pair<kotlinx.serialization.json.JsonElement, String>): RepositoryResult<Owner> = try {
        mutex.withLock {
            remoteResult {
                if (session.value.mode != AccountMode.SIGNED_OUT) throw RemoteFailure(RepositoryError.CONFLICT)
                val (body, action) = request()
                val response = api.request(null, "POST", listOf("auth", action), body)
                val detail = api.decode(response, AuthenticationDto.serializer())
                if (detail.tokenType != "Bearer" || !Instant.parse(detail.expiresAt).isAfter(Instant.now())) {
                    throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
                }
                val active = RemoteSession(detail.token, detail.owner.id)
                withContext(NonCancellable) {
                    clearAccount()
                    secure { storage.write(StoredSession(detail.token, active.ownerId, detail.expiresAt, api.server)) }
                    sessions.value = active
                    mutableSession.value = AccountSession(AccountMode.SERVER, active.ownerId)
                }
                detail.owner.toDomain(null)
            }
        }
    } finally {
        password.fill('\u0000')
    }

    override suspend fun logout(): RepositoryResult<Unit> = mutex.withLock {
        remoteResult {
            sessions.value?.let { active ->
                try {
                    api.request(active, "POST", listOf("auth", "logout"), JsonObject(emptyMap()))
                } catch (error: RemoteFailure) {
                    if (error.error != RepositoryError.AUTHENTICATION_REQUIRED) throw error
                }
            }
            clear()
        }
    }

    override suspend fun openLocalProfile(): RepositoryResult<Unit> = mutex.withLock {
        remoteResult {
            if (session.value.mode != AccountMode.SIGNED_OUT) throw RemoteFailure(RepositoryError.CONFLICT)
            val owner = value(localOwner.observeOwner().first()) ?: throw RemoteFailure(RepositoryError.INVALID_REFERENCE)
            secure { storage.clear() }
            clearAccount()
            value(modeStorage.setLocal(true))
            mutableSession.value = AccountSession(AccountMode.LOCAL, owner.id)
        }
    }

    suspend fun expire(expected: RemoteSession) = mutex.withLock {
        if (sessions.value === expected) remoteResult { clear() }
    }

    private suspend fun clear() = withContext(NonCancellable) {
        sessions.value = null
        try {
            secure { storage.clear() }
        } finally {
            try {
                try {
                    value(modeStorage.setLocal(false))
                } finally {
                    clearAccount()
                }
            } finally {
                mutableSession.value = AccountSession(AccountMode.SIGNED_OUT)
            }
        }
    }

    private fun <T> value(result: RepositoryResult<T>): T = when (result) {
        is RepositoryResult.Success -> result.value
        is RepositoryResult.Failure -> throw RemoteFailure(result.error, result.fieldErrors)
    }

    private suspend fun <T> secure(block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: GeneralSecurityException) {
        throw RemoteFailure(RepositoryError.SECURE_STORAGE_FAILED)
    } catch (_: ProviderException) {
        throw RemoteFailure(RepositoryError.SECURE_STORAGE_FAILED)
    } catch (_: SecurityException) {
        throw RemoteFailure(RepositoryError.SECURE_STORAGE_FAILED)
    } catch (_: IOException) {
        throw RemoteFailure(RepositoryError.SECURE_STORAGE_FAILED)
    } catch (_: IllegalStateException) {
        throw RemoteFailure(RepositoryError.SECURE_STORAGE_FAILED)
    } catch (_: IllegalArgumentException) {
        throw RemoteFailure(RepositoryError.SECURE_STORAGE_FAILED)
    }
}