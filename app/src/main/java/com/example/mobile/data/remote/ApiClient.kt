package com.example.mobile.data.remote

import com.example.mobile.BuildConfig
import com.example.mobile.data.remote.dto.ApiErrorEnvelope
import com.example.mobile.domain.model.AppLanguage
import com.example.mobile.domain.repository.RepositoryError
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

internal class ApiClient(
    baseUrl: String,
    private val operations: RemoteOperations,
    private val language: suspend () -> AppLanguage,
) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "type" }
    private val base = baseUrl.toHttpUrlOrNull()?.takeIf {
        (it.isHttps || BuildConfig.DEBUG) && it.username.isEmpty() && it.password.isEmpty() &&
            it.query == null && it.fragment == null
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    val server: String get() = base?.toString() ?: throw RemoteFailure(RepositoryError.NOT_CONFIGURED)
    internal val serverIdentity: String get() = base?.toString().orEmpty()

    suspend fun request(
        session: RemoteSession?,
        method: String,
        path: List<String>,
        body: JsonElement? = null,
        etag: String? = null,
        key: String? = null,
    ): ApiPayload {
        session?.let(operations::check)
        val url = (base ?: throw RemoteFailure(RepositoryError.NOT_CONFIGURED)).newBuilder()
        url.addPathSegment("api").addPathSegment("v1")
        path.forEach(url::addPathSegment)
        val builder = Request.Builder().url(url.build())
            .header("Accept", "application/json")
            .header("Accept-Language", language().languageTag)
        session?.let { builder.header("Authorization", it.authorization()) }
        etag?.let { builder.header("If-Match", it) }
        key?.let { builder.header("Idempotency-Key", it) }
        val response = execute(builder.method(method, body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())).build())
        session?.let(operations::check)
        if (response.status !in 200..299) {
            val detail = runCatching { json.decodeFromString<ApiErrorEnvelope>(response.body).error }.getOrNull()
            val error = when (response.status) {
                400, 422 -> RepositoryError.INVALID_DATA
                401 -> if (session == null) RepositoryError.INVALID_CREDENTIALS else RepositoryError.AUTHENTICATION_REQUIRED
                403 -> RepositoryError.ACCESS_DENIED
                404 -> RepositoryError.INVALID_REFERENCE
                409 -> if (detail?.code == "EMAIL_ALREADY_USED") RepositoryError.IDENTITY_CONFLICT else RepositoryError.CONFLICT
                412, 428 -> RepositoryError.VERSION_CONFLICT
                429 -> RepositoryError.RATE_LIMITED
                in 500..599 -> RepositoryError.SERVER_UNAVAILABLE
                else -> RepositoryError.INVALID_RESPONSE
            }
            throw RemoteFailure(error, detail?.fields.orEmpty(), response.status)
        }
        return response
    }

    fun <T> decode(payload: ApiPayload, serializer: DeserializationStrategy<T>): T = json.decodeFromString(serializer, payload.body)

    fun revision(response: ApiPayload): String =
        response.etag?.takeIf { it.startsWith('"') && it.endsWith('"') }
            ?: throw RemoteFailure(RepositoryError.INVALID_RESPONSE)

    fun version(revision: String?): String = revision ?: throw RemoteFailure(RepositoryError.VERSION_CONFLICT)

    private suspend fun execute(request: Request): ApiPayload = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val payload = response.use {
                        val source = it.body.source()
                        if (source.request(8L * 1024 * 1024 + 1)) throw RemoteFailure(RepositoryError.INVALID_RESPONSE)
                        ApiPayload(it.code, source.readUtf8(), it.header("ETag"))
                    }
                    if (continuation.isActive) continuation.resume(payload)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        })
    }
}

internal class ApiPayload(val status: Int, val body: String, val etag: String?)