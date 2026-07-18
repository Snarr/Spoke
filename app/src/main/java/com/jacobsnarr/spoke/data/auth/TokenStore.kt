package com.jacobsnarr.spoke.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists auth tokens in an EncryptedSharedPreferences file backed by the Android Keystore.
 * Works on de-Googled AOSP (Tink + Keystore, no Google Play Services required).
 *
 * Tokens are stored per bikeshare system (keyed by system id) so a user who travels between
 * cities can stay signed in to each system independently and switch without re-logging-in.
 */
class TokenStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences = try {
        buildEncryptedPrefs()
    } catch (_: Exception) {
        // The Keystore master key or the encrypted keyset is in an unreadable state (e.g.
        // corruption or a partial restore). Reset the store and rebuild once so the app
        // recovers gracefully — the user simply re-logs in — instead of crash-looping on
        // every launch (which looks like the app "resetting from scratch").
        appContext.deleteSharedPreferences(PREFS_FILE)
        buildEncryptedPrefs()
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey =
            MasterKey
                .Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun accessToken(systemId: String): String? = prefs.getString(accessKey(systemId), null)

    fun refreshToken(systemId: String): String? = prefs.getString(refreshKey(systemId), null)

    fun isLoggedIn(systemId: String): Boolean = !accessToken(systemId).isNullOrBlank() || !refreshToken(systemId).isNullOrBlank()

    fun expiresAtMillis(systemId: String): Long = prefs.getLong(expiresKey(systemId), 0L)

    fun isAccessTokenExpired(systemId: String, bufferMillis: Long = 0L): Boolean = isAccessTokenExpired(
        accessToken = accessToken(systemId),
        expiresAtMillis = expiresAtMillis(systemId),
        nowMillis = System.currentTimeMillis(),
        bufferMillis = bufferMillis,
    )

    fun saveSession(systemId: String, accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * MILLIS_PER_SECOND
        prefs.edit {
            putString(accessKey(systemId), accessToken)
            putString(refreshKey(systemId), refreshToken)
            putLong(expiresKey(systemId), expiresAt)
        }
    }

    fun clear(systemId: String) {
        prefs.edit {
            remove(accessKey(systemId))
            remove(refreshKey(systemId))
            remove(expiresKey(systemId))
        }
    }

    private fun accessKey(systemId: String) = "${KEY_ACCESS_TOKEN}_$systemId"

    private fun refreshKey(systemId: String) = "${KEY_REFRESH_TOKEN}_$systemId"

    private fun expiresKey(systemId: String) = "${KEY_EXPIRES_AT}_$systemId"

    private companion object {
        const val PREFS_FILE = "indego_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val MILLIS_PER_SECOND = 1000L
    }
}

internal fun isAccessTokenExpired(accessToken: String?, expiresAtMillis: Long, nowMillis: Long, bufferMillis: Long): Boolean {
    if (accessToken.isNullOrBlank()) return true
    return nowMillis >= (expiresAtMillis - bufferMillis)
}
