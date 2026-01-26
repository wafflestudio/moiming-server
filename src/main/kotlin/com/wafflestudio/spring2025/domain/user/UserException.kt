package com.wafflestudio.spring2025.domain.user

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatusCode

sealed class UserException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)
