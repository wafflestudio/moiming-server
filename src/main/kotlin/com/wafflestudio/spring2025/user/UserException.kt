package com.wafflestudio.spring2025.user

import com.wafflestudio.spring2025.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class UserException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class SignUpUsernameConflictException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Username conflict",
    )

class SignUpBadUsernameException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Bad username",
    )

class SignUpBadPasswordException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Bad password",
    )

class AuthenticateException :
    UserException(
        errorCode = 0,
        httpStatusCode = HttpStatus.UNAUTHORIZED,
        msg = "Authenticate failed",
    )

class UnregisteredSocialAccountException :
    UserException(
        errorCode = 4,
        httpStatusCode = HttpStatus.NOT_FOUND,
        msg = "User not found",
    )
