package com.wafflestudio.spring2025.domain.user.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    PROFILE_IMAGE_NOT_FOUND(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "프로필 이미지를 찾을 수 없습니다.",
        message = "존재하지 않는 이미지입니다.\n올바른 이미지 경로를 입력해 주세요.",
    ),
    EMAIL_CHANGE_FORBIDDEN(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "이메일은 변경할 수 없습니다.",
        message = "이메일 주소는 변경이 불가능합니다.",
    ),
}
