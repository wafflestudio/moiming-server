package com.wafflestudio.spring2025.common.email

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class EmailException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class EmailServiceUnavailableException :
    EmailException(
        errorCode = 4001,
        httpStatusCode = HttpStatus.SERVICE_UNAVAILABLE,
        msg = "Email service unavailable",
    )
