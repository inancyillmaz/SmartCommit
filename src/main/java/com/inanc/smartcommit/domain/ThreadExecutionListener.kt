package com.inanc.smartcommit.domain

interface ThreadExecutionListener {

    fun onStart()
    fun onEnd()

    fun onError(error: Throwable)
}
