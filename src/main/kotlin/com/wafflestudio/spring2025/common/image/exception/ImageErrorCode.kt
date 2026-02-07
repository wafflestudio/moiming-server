package com.wafflestudio.spring2025.common.image.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class ImageErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    IMAGE_FILE_EMPTY(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이미지 파일이 비어있습니다.",
        message = "업로드한 이미지가 비어있습니다. 파일을 다시 확인해 주세요.",
    ),
    IMAGE_FILE_TYPE_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "지원하지 않는 이미지 형식입니다.",
        message = "이미지 파일만 업로드할 수 있습니다.",
    ),
    IMAGE_FILE_TOO_LARGE(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이미지 파일 크기가 너무 큽니다.",
        message = "이미지 파일 크기는 5MB 이하로 제한되어 있습니다.",
    ),
}
