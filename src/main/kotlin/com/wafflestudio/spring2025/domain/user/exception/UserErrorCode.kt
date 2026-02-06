package com.wafflestudio.spring2025.domain.user.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    PROFILE_IMAGE_EMPTY(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이미지 파일이 비어있습니다.",
        message = "업로드된 이미지가 비어있습니다.\n파일을 다시 확인해 주세요.",
    ),
    PROFILE_IMAGE_FORMAT_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이미지 형식이 올바르지 않습니다.",
        message = "이미지 파일만 업로드할 수 있습니다.",
    ),
    PROFILE_IMAGE_TOO_LARGE(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이미지 크기가 너무 큽니다.",
        message = "이미지 파일 크기는 5MB 이하만 가능합니다.",
    ),
}
