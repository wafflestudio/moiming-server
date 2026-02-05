package com.wafflestudio.spring2025.common.email.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class EmailException(
    error: EmailErrorCode,
    cause: Throwable? = null,
) : DomainException(
        httpErrorCode = error.httpStatusCode,
        code = error,
        title = error.title,
        msg = error.message,
        cause = cause,
    )

class EmailServiceUnavailableException : EmailException(error = EmailErrorCode.EMAIL_SERVICE_UNAVAILABLE)
