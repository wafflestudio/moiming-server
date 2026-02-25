package com.wafflestudio.spring2025.domain.event.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

/**
 * 2000 ~ 2999 : Event domain error codes
 * - 20xx: Not Found
 * - 21xx: Forbidden
 * - 22xx: Validation (Bad Request)
 * - 23xx: Conflict
 */
enum class EventErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    EVENT_NOT_FOUND(
        httpStatusCode = HttpStatus.NOT_FOUND,
        title = "일정이 존재하지 않습니다.",
        message = "삭제된 일정이거나,\n링크 주소가 잘못되었습니다.",
    ),

    NOT_EVENT_ADMIN(
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "모임의 관리자가 아닙니다.",
        message = "모임을 변경할 권한이 없습니다.\n관리자 계정으로 로그인해 주세요.",
    ),

    // 22xx - Validation / Bad Request
    // 제목 검증
    EVENT_TITLE_BLANK(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "제목을 입력해 주세요.",
        message = "모임 제목은 비워둘 수 없습니다.",
    ),

    // 정원 검증
    EVENT_CAPACITY_REQUIRED(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "정원을 입력해 주세요.",
        message = "정원은 필수값입니다.",
    ),
    EVENT_CAPACITY_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "정원이 유효하지 않습니다.",
        message = "정원은 1 이상이어야 합니다.",
    ),

    // 모임 시간 검증
    EVENT_STARTS_IN_PAST(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모임 시작 시간이 유효하지 않습니다.",
        message = "모임 시작 시간은\n현재 시간 이후여야 합니다.",
    ),
    EVENT_ENDS_IN_PAST(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모임 종료 시간이 유효하지 않습니다.",
        message = "모임 종료 시간은\n현재 시간 이후여야 합니다.",
    ),
    EVENT_TIME_RANGE_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모임 시간이 유효하지 않습니다.",
        message = "모임 종료 시간이 모임 시작 시간보다\n빠를 수 없습니다.",
    ),

    // 신청 기간 검증
    REGISTRATION_STARTS_IN_PAST(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "신청 시작 시간이 유효하지 않습니다.",
        message = "신청 시작 시간은\n현재 시간 이후여야 합니다.",
    ),
    REGISTRATION_ENDS_IN_PAST(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "신청 마감 시간이 유효하지 않습니다.",
        message = "신청 마감 시간은\n현재 시간 이후여야 합니다.",
    ),
    REGISTRATION_TIME_RANGE_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "신청 기간이 유효하지 않습니다.",
        message = "신청 마감 시간이 신청 시작 시간보다\n빠를 수 없습니다.",
    ),
    REGISTRATION_STARTS_AFTER_EVENT_START(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "신청 기간이 유효하지 않습니다.",
        message = "신청 시작 시간이 모임 시작 시간보다\n빨라야 합니다.",
    ),
    REGISTRATION_ENDS_AFTER_EVENT_START(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "신청 기간이 유효하지 않습니다.",
        message = "신청 마감 시간이 모임 시작 시간보다\n빨라야 합니다.",
    ),

    // 23xx - Conflict
    EVENT_FULL(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "정원이 초과되었습니다.",
        message = "모임 정원이 가득 차\n더 이상 신청할 수 없습니다.",
    ),
}
