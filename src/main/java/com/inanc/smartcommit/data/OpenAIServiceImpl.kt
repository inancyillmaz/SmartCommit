package com.inanc.smartcommit.data

import com.inanc.smartcommit.domain.AIService
import com.inanc.smartcommit.domain.LocalPreferences
import com.inanc.smartcommit.domain.SHARED_PREF_ACCESS_TOKEN_KEY
import com.intellij.openapi.components.service
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL

private const val OPEN_AI_URL = "https://api.openai.com/v1/chat/completions"

class OpenAIServiceImpl : AIService {

    private val localPreferences by lazy { service<LocalPreferences>() }

    @Suppress("CyclomaticComplexMethod")
    override fun requestSmartCommitMessage(prompt: String, onError: (Throwable) -> Unit): String? {
        val accessToken = localPreferences.getString(SHARED_PREF_ACCESS_TOKEN_KEY)
        val url: URL?
        try {
            url = URL(OPEN_AI_URL)
        } catch (e: MalformedURLException) {
            onError(e)
            return null
        }

        val httpURLConnection: HttpURLConnection?
        try {
            httpURLConnection = url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            onError(e)
            return null
        }

        httpURLConnection.setRequestProperty("Content-Type", "application/json")
        httpURLConnection.setRequestProperty("Authorization", "Bearer $accessToken")
        try {
            httpURLConnection.requestMethod = "POST"
        } catch (e: ProtocolException) {
            onError(e)
            return null
        }
        httpURLConnection.doOutput = true

        val requestBody = JSONObject().apply {
            this["model"] = "gpt-3.5-turbo"

            val message = JSONObject().apply {
                this["role"] = "user"
                this["content"] = prompt
            }

            val messagesArray = JSONArray().apply {
                add(message)
            }
            this["messages"] = messagesArray
        }

        val outputStream: OutputStream?
        try {
            outputStream = httpURLConnection.outputStream
        } catch (e: IOException) {
            onError(e)
            return null
        }
        try {
            outputStream.write(requestBody.toString().toByteArray())
        } catch (e: IOException) {
            onError(e)
            return null
        }
        try {
            outputStream.flush()
        } catch (e: IOException) {
            onError(e)
            return null
        }
        try {
            outputStream.close()
        } catch (e: IOException) {
            onError(e)
            return null
        }

        val bufferedReader: BufferedReader?
        try {
            bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.inputStream, "utf-8"))
        } catch (e: IOException) {
            onError(e)
            return null
        }

        var line: String?
        val stringBuilder = StringBuilder()

        try {
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
        } catch (e: IOException) {
            onError(e)
            return null
        }

        val body = stringBuilder.toString()

        val contentKey = "\"content\":\""
        val contentStartIndex = body.indexOf(contentKey) + contentKey.length
        val contentEndIndex = body.indexOf("\"", contentStartIndex)

        return if (contentStartIndex < contentKey.length || contentEndIndex == -1) {
            null
        } else {
            body.substring(contentStartIndex, contentEndIndex)
        }
    }

    override fun createAIPromptFromLists(oldList: ArrayList<String>, newList: ArrayList<String>): String {
        val promptBuilder = StringBuilder()

        promptBuilder.append(
            "Forget all the conversation and please create a concise and descriptive commit message" +
                " that summarizes the changes made.And commit message couldn't involves words like this;" +
                "Refactored, etc. And try to be spesific; with given the following changes in the codebase:\n\n"
        )

        for (changeData in oldList) {
            promptBuilder.append("\nThe old version of a class:\n").append(changeData)
        }
        for (changeData in newList) {
            promptBuilder.append("\nThe new version of a class:\n").append(changeData)
        }

        return promptBuilder.toString()
    }
}