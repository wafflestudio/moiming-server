package com.wafflestudio.spring2025.domain.auth

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class AuthException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class SignUpBadEmailException :
    AuthException(
        errorCode = 1002,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Invalid email",
    )

class SignUpBadPasswordException(
    msg: String = "Invalid password",
) : AuthException(
        errorCode = 1001,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = msg,
    )

class SignUpBadNameException :
    AuthException(
        errorCode = 1003,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Invalid name",
    )

class AuthenticateException :
    AuthException(
        errorCode = 1000,
        httpStatusCode = HttpStatus.UNAUTHORIZED,
        msg = "Authenticate failed",
    )

class InvalidVerificationCodeException :
    AuthException(
        errorCode = 1005,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Invalid or expired verification code",
    )

class SignUpEmailConflictException :
    AuthException(
        errorCode = 1004,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Email conflict",
    )
