package com.inanc.smartcommit

import java.util.prefs.Preferences

class LocalStorage {

    private val prefs: Preferences = Preferences.userNodeForPackage(LocalStorage::class.java)

    // Save a value
    fun saveValue(key: String, value: String) {
        prefs.put(key, value)
    }

    fun removeValue(key: String) {
        prefs.remove(key)
    }

    // Load a value
    fun loadValue(key: String): String? {
        return prefs.get(key, null) // null is the default value if key is not found
    }

    fun saveTerms(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    fun loadValueAcceptTerms(acceptTerms: String): Boolean {
        return prefs.getBoolean(acceptTerms, false)
    }
}
