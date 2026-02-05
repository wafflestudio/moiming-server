package com.wafflestudio.spring2025.domain.auth.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class AuthException(
    error: AuthErrorCode,
    cause: Throwable? = null,
) : DomainException(
    httpErrorCode = error.httpStatusCode,
    code = error,
    title = error.title,
    msg = error.message,
    cause = cause,
)

class AuthValidationException(
    error: AuthErrorCode,
) : AuthException(error = error)

class AuthenticationFailedException(
    cause: Throwable? = null,
) : AuthException(error = AuthErrorCode.AUTHENTICATION_ERROR, cause = cause)

class AuthenticationRequiredException : AuthException(error = AuthErrorCode.AUTHENTICATION_REQUIRED)

class AccountAlreadyExistsException(
    error: AuthErrorCode,
) : AuthException(error = error)

class GoogleOAuthException(
    cause: Throwable? = null,
) : AuthException(error = AuthErrorCode.GOOGLE_OAUTH_ERROR, cause = cause)