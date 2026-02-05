package com.wafflestudio.spring2025.domain.registration.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class RegistrationException(
    error: RegistrationErrorCode,
    cause: Throwable? = null,
) : DomainException(
    httpErrorCode = error.httpStatusCode,
    code = error,
    title = error.title,
    msg = error.message,
    cause = cause,
)

class RegistrationNotFoundException : RegistrationException(error = RegistrationErrorCode.REGISTRATION_NOT_FOUND)

class RegistrationForbiddenException(
    error: RegistrationErrorCode,
) : RegistrationException(error = error)

class RegistrationValidationException(
    error: RegistrationErrorCode,
) : RegistrationException(error = error)

class RegistrationConflictException(
    error: RegistrationErrorCode,
) : RegistrationException(error = error)