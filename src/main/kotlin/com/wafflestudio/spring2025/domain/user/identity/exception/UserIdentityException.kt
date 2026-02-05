package com.wafflestudio.spring2025.domain.user.identity.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class UserIdentityException(
    error: UserIdentityErrorCode,
    cause: Throwable? = null,
) : DomainException(
        httpErrorCode = error.httpStatusCode,
        code = error,
        title = error.title,
        msg = error.message,
        cause = cause,
    )

class UserIdentityNotFoundException : UserIdentityException(error = UserIdentityErrorCode.USER_IDENTITY_NOT_FOUND)

class UserIdentityAlreadyLinkedException : UserIdentityException(error = UserIdentityErrorCode.USER_IDENTITY_ALREADY_LINKED)

class UserIdentityUnauthorizedException : UserIdentityException(error = UserIdentityErrorCode.USER_IDENTITY_UNAUTHORIZED)
