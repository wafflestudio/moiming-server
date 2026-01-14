package com.wafflestudio.spring2025.domain.user

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class UserException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class InvalidVerificationCodeException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Invalid or expired verification code",
    )

class VerificationCodeExpiredException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Verification code has expired",
    )

class EmailAlreadyExistsException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Email already exists",
    )
