package com.inanc.smartcommit.domain

import com.google.gson.JsonParser
import com.inanc.smartcommit.PluginBundle
import com.inanc.smartcommit.presentation.notifyErrorMessage
import com.intellij.openapi.project.Project
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

fun String.getWordsCount(): Int {
    if (this.isEmpty()) {
        return 0
    }
    val words = this.trim { it <= ' ' }.split("\\s+".toRegex()).toTypedArray()
    return words.size
}

fun String.openWebURL(onError: (errorMessage: String?) -> Unit) {
    try {
        Desktop.getDesktop().browse(URI(this))
    } catch (e: IOException) {
        onError(e.message)
    } catch (e: URISyntaxException) {
        onError(e.message)
    }
}

fun String.extractContent(project: Project): String? {
    return try {
        val jsonObject = JsonParser.parseString(this).asJsonObject
        jsonObject.getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
    } catch (e: Exception) {
        project.notifyErrorMessage(
            displayId = PluginBundle.message("error"),
            title = e.message.toString(),
            message =  e.message.toString(),
            shouldInvokeLater = false
        )
        null
    }
}


@Suppress("TooGenericExceptionCaught")
fun String?.extractAccessToken(): String? = try {
    val jsonObj = JsonParser.parseString(this).asJsonObject
    jsonObj.get("accessToken")?.asString
} catch (_: Exception) {
    null
}