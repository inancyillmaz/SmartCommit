package com.inanc.smartcommit.data.exceptions

sealed interface ApiExceptions {
    object ApiExceptions429 : ApiExceptions
    object ApiExceptions401 : ApiExceptions
    object ApiExceptionsUnknown : ApiExceptions
}
