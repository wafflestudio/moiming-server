package com.wafflestudio.spring2025.common.email.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class EmailErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    EMAIL_SERVICE_UNAVAILABLE(
        httpStatusCode = HttpStatus.SERVICE_UNAVAILABLE,
        title = "이메일 서비스를 이용할 수 없습니다.",
        message = "잠시 후 다시 시도해주세요.\n오류가 지속되면 개발자에게 문의해 주세요.",
    ),
}