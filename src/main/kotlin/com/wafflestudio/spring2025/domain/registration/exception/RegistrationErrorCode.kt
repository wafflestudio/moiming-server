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
    REGISTRATION_PATCH_UNAUTHORIZED(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "신청 상태 변경 권한이 없습니다.",
        message = "모임의 관리자 혹은 신청자 본인만\n신청 상태를 변경할 수 있습니다.",
    ),

    REGISTRATION_DELETE_UNAUTHORIZED(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "신청 삭제 권한이 없습니다.",
        message = "모임의 관리자 혹은 신청자 본인만\n신청을 삭제할 수 있습니다.",
    ),

    REGISTRATION_VIEW_UNAUTHORIZED(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "열람 권한이 없습니다.",
        message = "본인의 신청 정보만 조회할 수 있습니다.",
    ),

    REGISTRATION_INVALID_TOKEN(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "유효하지 않은 신청 토큰입니다.",
        message = "토큰이 만료되었거나 잘못되었습니다.\n다시 시도해 주세요.",
    ),

    // 32xx - Validation (Bad Request)
    REGISTRATION_WRONG_NAME(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이름 형식이 올바르지 않습니다.",
        message = "최소 2글자 이상 입력해주세요.",
    ),

    REGISTRATION_WRONG_EMAIL(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이메일 형식이 올바르지 않습니다.",
        message = "모임 참여 정보를 받을 수 있도록\n이메일 주소를 정확하게 입력해 주세요.",
    ),

    REGISTRATION_INVALID_STATUS(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "유효하지 않은 신청 상태입니다.",
        message = "현재 상태에서 변경할 수 없는\n신청 상태입니다.",
    ),

    REGISTRATION_BLOCKED_HOST(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "주최자는 신청할 수 없습니다.",
        message = "해당 모임의 주최자는\n참여 신청할 수 없습니다.",
    ),

    REGISTRATION_BLOCKED_BANNED(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "차단된 신청입니다.",
        message = "차단된 신청은\n다시 신청할 수 없습니다.",
    ),

    NOT_WITHIN_REGISTRATION_WINDOW(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모집 기간이 아닙니다.",
        message = "현재 모집 기간이 아니므로\n신청 혹은 정정이 불가합니다.",
    ),

    // 33xx - Conflict
    REGISTRATION_ALREADY_EXISTS(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "이미 신청하셨습니다.",
        message = "동일한 모임에 중복으로\n신청할 수 없습니다.",
    ),

    REGISTRATION_ALREADY_BANNED(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "차단된 신청입니다.",
        message = "해당 신청은 차단 처리되어\n변경할 수 없습니다.",
    ),

    INVALID_REGISTRATION_QUERY_PARAMETER(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "신청자 필터 조건이 올바르지 않습니다.",
        message = "유효하지 않은 쿼리 파라미터 값입니다.",
    ),
}
