package com.cometchat.uikit.core.hub

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Token + device state, encrypted at rest. One instance per process, created
 * by [Hub]. Token rides `Authorization: Bearer` on REST and the WS upgrade.
 */
public class HubStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gochathub_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    public var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    public var token: String
        get() = prefs.getString(KEY_TOKEN, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    /** Server base URL; last used login wins. */
    public var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    public fun wipe() {
        deviceId = ""
        token = ""
    }

    public fun hasSession(): Boolean = token.isNotEmpty()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_BASE_URL = "base_url"
    }
}