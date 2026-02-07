package com.wafflestudio.spring2025.domain.user.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class UserException(
    error: UserErrorCode,
    cause: Throwable? = null,
) : DomainException(
        httpErrorCode = error.httpStatusCode,
        code = error,
        title = error.title,
        msg = error.message,
        cause = cause,
    )

class UserValidationException(
    error: UserErrorCode,
) : UserException(error = error)

class EmailChangeForbiddenException : UserException(error = UserErrorCode.EMAIL_CHANGE_FORBIDDEN)
