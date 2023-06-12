package com.inanc.smartcommit.data

import com.inanc.smartcommit.PluginBundle
import com.inanc.smartcommit.data.exceptions.ApiExceptions
import com.inanc.smartcommit.domain.LocalPreferences
import com.inanc.smartcommit.domain.OpenAIService
import com.inanc.smartcommit.domain.SHARED_PREF_ACCESS_TOKEN_KEY
import com.inanc.smartcommit.domain.extractAccessToken
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

class OpenAIServiceImpl : OpenAIService {

    private val localPreferences by lazy { service<LocalPreferences>() }

    @Suppress("CyclomaticComplexMethod")
    override fun requestSmartCommitMessage(prompt: String, onError: (ApiExceptions) -> Unit): String? {
        val accessToken = localPreferences.getString(SHARED_PREF_ACCESS_TOKEN_KEY).extractAccessToken()

        val url: URL?
        try {
            url = URL(OPEN_AI_URL)
        } catch (e: MalformedURLException) {
            onError(ApiExceptions.ApiExceptionsUnknown)
            return null
        }

        val httpURLConnection: HttpURLConnection?
        try {
            httpURLConnection = url.openConnection() as HttpURLConnection
        } catch (_: IOException) {
            onError(ApiExceptions.ApiExceptionsUnknown)
            return null
        }

        httpURLConnection.setRequestProperty("Content-Type", "application/json")
        httpURLConnection.setRequestProperty("Authorization", "Bearer $accessToken")
        try {
            httpURLConnection.requestMethod = "POST"
        } catch (_: ProtocolException) {
            onError(ApiExceptions.ApiExceptionsUnknown)
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
        } catch (_: IOException) {
            onError(createApiApiExceptions(httpURLConnection.responseCode))
            return null
        }
        try {
          val result =  outputStream.write(requestBody.toString().toByteArray())
        } catch (_: IOException) {
            onError(createApiApiExceptions(httpURLConnection.responseCode))
            return null
        }
        try {
            outputStream.flush()
        } catch (_: IOException) {
            onError(createApiApiExceptions(httpURLConnection.responseCode))
            return null
        }
        try {
            outputStream.close()
        } catch (_: IOException) {
            onError(createApiApiExceptions(httpURLConnection.responseCode))
            return null
        }

        val bufferedReader: BufferedReader?
        try {
            bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.inputStream, "utf-8"))
        } catch (_: IOException) {
            onError(createApiApiExceptions(httpURLConnection.responseCode))
            return null
        }

        var line: String?
        val stringBuilder = StringBuilder()

        try {
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
        } catch (_: IOException) {
            onError(ApiExceptions.ApiExceptionsUnknown)
            return null
        }

        return stringBuilder.toString()
    }

    override fun createAIPromptFromLists(oldList: ArrayList<String>, newList: ArrayList<String>): String {
        val promptBuilder = StringBuilder()

        promptBuilder.append(
            PluginBundle.message("aiPrompt")
        )

        for (changeData in oldList) {
            promptBuilder.append("\nThe old version of a class:\n").append(changeData)
        }
        for (changeData in newList) {
            promptBuilder.append("\nThe new version of a class:\n").append(changeData)
        }

        return promptBuilder.toString()
    }

    private fun createApiApiExceptions(code: Int): ApiExceptions {
        return when (code) {
            429 -> ApiExceptions.ApiExceptions429
            401 -> ApiExceptions.ApiExceptions401
            else -> ApiExceptions.ApiExceptionsUnknown
        }
    }
}
