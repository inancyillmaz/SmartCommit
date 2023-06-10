package com.inanc.smartcommit.domain

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
