package com.inanc.smartcommit.domain

import com.inanc.smartcommit.data.exceptions.ApiExceptions

interface ThreadExecutionListener {

    fun onStart()
    fun onEnd()
    fun onError(error: Throwable)
    fun onApiError(apiExceptions: ApiExceptions)
}
