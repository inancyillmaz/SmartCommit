package com.inanc.smartcommit.data

import com.inanc.smartcommit.domain.AIService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BardAIServiceImpl : AIService {
    override fun requestSmartCommitMessage(prompt: String, onError: (Throwable) -> Unit): String? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.bard.ai/v1/generate")
            .header("Content-Type", "application/json")
            .post("{\"text\": \"$prompt\"}".toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string()
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
