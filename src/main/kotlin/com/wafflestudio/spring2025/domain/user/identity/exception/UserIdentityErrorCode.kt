package com.wafflestudio.spring2025.domain.user.identity.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class UserIdentityErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    USER_IDENTITY_NOT_FOUND(
        httpStatusCode = HttpStatus.NOT_FOUND,
        title = "사용자 정보를 찾을 수 없습니다.",
        message = "존재하지 않는 사용자이거나,\n계정 정보가 삭제되었습니다.",
    ),

    USER_IDENTITY_ALREADY_LINKED(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "이미 연동된 계정입니다.",
        message = "해당 소셜 계정은 이미\n다른 계정에 연동되어 있습니다.",
    ),

    USER_IDENTITY_UNAUTHORIZED(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "접근 권한이 없습니다.",
        message = "해당 사용자 정보에 대한\n접근 권한이 없습니다.",
    ),
}
