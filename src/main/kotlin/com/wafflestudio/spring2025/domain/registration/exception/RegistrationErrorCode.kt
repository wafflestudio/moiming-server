package com.wafflestudio.spring2025.domain.registration.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

/**
 * 3000 ~ 3999 : Registration domain error codes
 * - 30xx: Not Found
 * - 31xx: Forbidden
 * - 32xx: Validation (Bad Request)
 * - 33xx: Conflict
 */
enum class RegistrationErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    // 30xx - Not Found
    REGISTRATION_NOT_FOUND(
        httpStatusCode = HttpStatus.NOT_FOUND,
        title = "신청 내역을 찾을 수 없습니다.",
        message = "삭제된 신청이거나,\n존재하지 않는 신청입니다.",
    ),

    // 31xx - Forbidden
    REGISTRATION_UNAUTHORIZED(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "신청을 관리할 권한이 없습니다.",
        message = "본인의 신청이 아니거나,\n관리 권한이 없는 모임입니다.",
    ),

    REGISTRATION_INVALID_TOKEN(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "유효하지 않은 신청 토큰입니다.",
        message = "토큰이 만료되었거나 잘못되었습니다.\n다시 시도해 주세요.",
    ),

    // 32xx - Validation (Bad Request)
    REGISTRATION_WRONG_NAME(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이름이 일치하지 않습니다.",
        message = "신청 시 입력한 이름과\n일치하지 않습니다.",
    ),

    REGISTRATION_WRONG_EMAIL(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이메일이 일치하지 않습니다.",
        message = "신청 시 입력한 이메일과\n일치하지 않습니다.",
    ),

    REGISTRATION_INVALID_STATUS(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "유효하지 않은 신청 상태입니다.",
        message = "현재 상태에서 변경할 수 없는\n신청 상태입니다.",
    ),

    NOT_WITHIN_REGISTRATION_WINDOW(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모집 기간이 아닙니다.",
        message = "현재 모집 기간이 아니므로\n신청할 수 없습니다.",
    ),

    // 33xx - Conflict
    REGISTRATION_ALREADY_EXISTS(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "이미 신청하셨습니다.",
        message = "동일한 모임에 중복으로\n신청할 수 없습니다.",
    ),

    REGISTRATION_ALREADY_CANCELED(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "이미 취소된 신청입니다.",
        message = "해당 신청은 이미 취소되었습니다.",
    ),

    REGISTRATION_ALREADY_BANNED(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "차단된 신청입니다.",
        message = "해당 신청은 차단 처리되어\n변경할 수 없습니다.",
    ),
}