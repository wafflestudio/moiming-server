package com.wafflestudio.spring2025.domain.auth.exception

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class UserException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class SignUpEmailConflictException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Email conflict",
    )

class SignUpBadEmailException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Bad email",
    )

class AuthenticateException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.UNAUTHORIZED,
        msg = "Authenticate failed",
    )
