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
    EMAIL_SERVICE_TEMPORARY_DOWN(
        httpStatusCode = HttpStatus.SERVICE_UNAVAILABLE,
        title = "메일링 기능 보수 작업 중입니다.",
        message = "이메일 회원가입은 인증 없이 바로 가입 완료됩니다.\n참여, 대기 확정, 취소 확인 메일은 발송되지 않습니다.",
    ),
}
