package com.inanc.smartcommit.domain

interface OpenAIService {

    fun requestSmartCommitMessage(prompt: String, onError: (Throwable) -> Unit): String?
    fun createAIPromptFromLists(oldList: ArrayList<String>, newList: ArrayList<String>): String
}
