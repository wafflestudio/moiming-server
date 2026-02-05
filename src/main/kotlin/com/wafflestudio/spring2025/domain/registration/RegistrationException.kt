package com.wafflestudio.spring2025.domain.registration

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class RegistrationException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class RegistrationNotFoundException :
    RegistrationException(
        errorCode = 3000,
        httpStatusCode = HttpStatus.NOT_FOUND,
        msg = "Registration not found",
    )

class RegistrationAlreadyExistsException :
    RegistrationException(
        errorCode = 3001,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Registration already exists",
    )

class RegistrationUnauthorizedException :
    RegistrationException(
        errorCode = 3002,
        httpStatusCode = HttpStatus.FORBIDDEN,
        msg = "Not authorized to manage registrations",
    )

class RegistrationWrongNameException :
    RegistrationException(
        errorCode = 3003,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Wrong guest name",
    )

class RegistrationWrongEmailException :
    RegistrationException(
        errorCode = 3004,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Wrong guest email",
    )

class RegistrationInvalidStatusException :
    RegistrationException(
        errorCode = 3005,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Invalid registration status",
    )

class RegistrationInvalidTokenException :
    RegistrationException(
        errorCode = 3006,
        httpStatusCode = HttpStatus.FORBIDDEN,
        msg = "Invalid registration token",
    )

class RegistrationAlreadyCanceledException :
    RegistrationException(
        errorCode = 3007,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Registration already canceled",
    )

class RegistrationAlreadyBannedException :
    RegistrationException(
        errorCode = 3008,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Registration already banned",
    )
