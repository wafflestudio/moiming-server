package com.wafflestudio.spring2025.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

open class DomainException(
    val errorCode: Int,
    val httpErrorCode: HttpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
    val msg: String,
    cause: Throwable? = null,
) : RuntimeException(msg, cause) {
    override fun toString(): String = "DomainException(msg='$msg', errorCode=$errorCode, httpErrorCode=$httpErrorCode)"
}