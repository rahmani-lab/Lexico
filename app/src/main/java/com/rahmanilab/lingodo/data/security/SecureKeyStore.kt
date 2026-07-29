package com.rahmanilab.lingodo.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

private val Context.secureStore: DataStore<Preferences> by preferencesDataStore(name = "secure")

/**
 * Encrypts small secrets (BYOK API keys) with an AES-256-GCM key held in the hardware-backed
 * **Android Keystore**, and stores only the ciphertext + IV in DataStore. Plaintext keys never touch
 * disk. The Keystore key is non-exportable, so the secrets are bound to this device/app install.
 */
class SecureKeyStore(private val context: Context) {

    suspend fun putSecret(name: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        context.secureStore.edit {
            it[ivKey(name)] = Base64.encodeToString(iv, Base64.NO_WRAP)
            it[ctKey(name)] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        }
    }

    suspend fun getSecret(name: String): String? {
        val prefs = context.secureStore.data.first()
        val ivB64 = prefs[ivKey(name)] ?: return null
        val ctB64 = prefs[ctKey(name)] ?: return null
        return runCatching {
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val ct = Base64.decode(ctB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            }
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.getOrNull()
    }

    suspend fun hasSecret(name: String): Boolean {
        val prefs = context.secureStore.data.first()
        return prefs[ctKey(name)] != null
    }

    suspend fun clearSecret(name: String) {
        context.secureStore.edit {
            it.remove(ivKey(name))
            it.remove(ctKey(name))
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun ivKey(name: String) = stringPreferencesKey("${name}_iv")
    private fun ctKey(name: String) = stringPreferencesKey("${name}_ct")

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lingodo_secret_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
