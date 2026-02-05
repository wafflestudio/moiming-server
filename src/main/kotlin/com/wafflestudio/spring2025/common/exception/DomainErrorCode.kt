package com.wafflestudio.spring2025.common.exception

import org.springframework.http.HttpStatus

interface DomainErrorCode {
    val name: String
    val httpStatusCode: HttpStatus
    val title: String
    val message: String
}
