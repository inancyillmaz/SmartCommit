package com.inanc.smartcommit.data

import com.inanc.smartcommit.domain.LocalPreferences
import java.util.prefs.Preferences

class LocalPreferenceImpl : LocalPreferences {

    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(LocalPreferences::class.java)
    }

    override fun saveString(key: String, value: String) {
        prefs.put(key, value)
    }

    override fun getString(key: String): String? {
        return prefs.get(key, null)
    }

    override fun saveBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    override fun getBoolean(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }
}
