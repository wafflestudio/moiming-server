package com.wafflestudio.spring2025.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

open class DomainException(
    val httpErrorCode: HttpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
    val code: DomainErrorCode,
    val title: String,
    val msg: String,
    cause: Throwable? = null,
) : RuntimeException(msg, cause) {
    override fun toString(): String = "DomainException(code=$code, title=$title, msg='$msg', httpErrorCode=$httpErrorCode)"
}
