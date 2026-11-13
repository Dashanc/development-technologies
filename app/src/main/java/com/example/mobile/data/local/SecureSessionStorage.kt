package com.example.mobile.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal class StoredSession(val token: String, val ownerId: String, val expiresAt: String, val server: String)

internal class SecureSessionStorage(context: Context) {
    private val path = File(context.noBackupFilesDir, "server_session_v1")
    private val backup = File(path.path + ".bak")
    private val file = AtomicFile(path)
    private val alias = "murpassport.session.v1"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        if (!path.exists() && !backup.exists()) return@withContext null
        val bytes = file.readFully()
        require(bytes.size in 29..16384)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
        val plain = cipher.doFinal(bytes, 12, bytes.size - 12)
        try {
            json.decodeFromString<StoredSession>(plain.toString(Charsets.UTF_8))
        } finally {
            plain.fill(0)
        }
    }

    suspend fun write(session: StoredSession) = withContext(Dispatchers.IO) {
        val plain = json.encodeToString(session).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val bytes = try {
            cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
            cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
            cipher.iv + cipher.doFinal(plain)
        } finally {
            plain.fill(0)
        }
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
        val saved = read()
        check(saved != null && saved.token == session.token && saved.ownerId == session.ownerId &&
            saved.expiresAt == session.expiresAt && saved.server == session.server)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        check(!path.exists() && !backup.exists())
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}