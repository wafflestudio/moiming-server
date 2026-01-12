package com.wafflestudio.spring2025.domain.user.identity

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class UserIdentityException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class UserIdentityNotFoundException : UserIdentityException(
    errorCode = 0,
    httpStatusCode = HttpStatus.NOT_FOUND,
    msg = "User identity not found",
)

class UserIdentityAlreadyLinkedException : UserIdentityException(
    errorCode = 0,
    httpStatusCode = HttpStatus.CONFLICT,
    msg = "User identity already linked",
)

class UserIdentityUnauthorizedException : UserIdentityException(
    errorCode = 0,
    httpStatusCode = HttpStatus.FORBIDDEN,
    msg = "Not authorized to access this user identity",
)
