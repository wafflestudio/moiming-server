package com.wafflestudio.spring2025.common.image.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class ImageException(
    error: ImageErrorCode,
    cause: Throwable? = null,
) : DomainException(
        httpErrorCode = error.httpStatusCode,
        code = error,
        title = error.title,
        msg = error.message,
        cause = cause,
    )

class ImageValidationException(
    error: ImageErrorCode,
) : ImageException(error = error)
