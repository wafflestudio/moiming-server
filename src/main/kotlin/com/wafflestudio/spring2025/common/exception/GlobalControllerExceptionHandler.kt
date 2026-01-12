package com.wafflestudio.spring2025.common.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalControllerExceptionHandler {
    @ExceptionHandler(DomainException::class)
    fun handle(exception: DomainException): ResponseEntity<Map<String, Any>> =
        ResponseEntity
            .status(exception.httpErrorCode)
            .body(mapOf("error" to exception.msg, "errorCode" to exception.errorCode))
}