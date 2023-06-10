package com.inanc.smartcommit.domain

import com.inanc.smartcommit.data.exceptions.ApiExceptions

interface OpenAIService {

    fun requestSmartCommitMessage(prompt: String, onError: (ApiExceptions) -> Unit): String?
    fun createAIPromptFromLists(oldList: ArrayList<String>, newList: ArrayList<String>): String
}
