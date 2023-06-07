package com.inanc.smartcommit.domain

interface LocalPreferences {

    fun saveString(key: String, value: String)

    fun getString(key: String): String?

    fun saveBoolean(key: String, value: Boolean)

    fun getBoolean(key: String): Boolean
}
