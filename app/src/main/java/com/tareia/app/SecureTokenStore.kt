package com.tareia.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts the Moodle web-service token at rest with a non-exportable Android Keystore key. */
object SecureTokenStore {
    private const val PREF = "tareia_moodle"
    private const val KEY_CIPHER = "encrypted_token"
    private const val KEY_ALIAS = "tareia_moodle_token_key_v1"

    @Synchronized
    fun write(context: Context, value: String) {
        writeProtected(context, PREF, KEY_CIPHER, value)
    }

    @Synchronized
    fun writeProtected(context: Context, preferenceFile: String, key: String, value: String) {
        if (value.isBlank()) return
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD("$preferenceFile:$key".toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        context.getSharedPreferences(preferenceFile, Context.MODE_PRIVATE).edit().putString(key, packed).apply()
    }

    @Synchronized
    fun read(context: Context): String = readProtected(context, PREF, KEY_CIPHER)

    @Synchronized
    fun readProtected(context: Context, preferenceFile: String, key: String): String {
        val packed = context.getSharedPreferences(preferenceFile, Context.MODE_PRIVATE).getString(key, null) ?: return ""
        return runCatching {
            val parts = packed.split(":", limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.updateAAD("$preferenceFile:$key".toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY_CIPHER).apply()
    }

    @Synchronized
    fun clearProtected(context: Context, preferenceFile: String, key: String) {
        context.getSharedPreferences(preferenceFile, Context.MODE_PRIVATE).edit().remove(key).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }
}
