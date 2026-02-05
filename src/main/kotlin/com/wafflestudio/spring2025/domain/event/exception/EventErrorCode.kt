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
        message = "삭제된 일정이거나,\n링크 주소가 잘못되었습니다."
    ),

    NOT_EVENT_ADMIN (
        httpStatusCode = HttpStatus.FORBIDDEN,
        title = "모임의 관리자가 아닙니다.",
        message = "모임을 변경할 권한이 없습니다.\n관리자 계정으로 로그인해 주세요."
    ),

    // 22xx - Validation / Bad Request
    EVENT_TITLE_BLANK(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "제목을 입력해 주세요.",
        message = "모임 제목은 비워둘 수 없습니다."
    ),

    EVENT_TIME_RANGE_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모임 시각이 유효하지 않습니다.",
        message = "종료 시각이 시작 시각보다\n빠를 수 없습니다."
    ),

    EVENT_CAPACITY_INVALID(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "정원이 유효하지 않습니다.",
        message = "정원은 1 이상이어야 합니다."
    ),

    REGISTRATION_ENDS_BEFORE_STARTS(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모집 기간이 유효하지 않습니다.",
        message = "모집 마감 시각이 모집 시작 시각보다\n빠를 수 없습니다."
    ),

    REGISTRATION_ENDS_AFTER_EVENT_START(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모집 기간이 유효하지 않습니다.",
        message = "모집 마감 시각이 모임 시작 시각보다\n늦을 수 없습니다."
    ),

    REGISTRATION_STARTS_AFTER_EVENT_START(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모집 기간이 유효하지 않습니다.",
        message = "모집 시작 시각이 모임 시작 시각보다\n늦을 수 없습니다."
    ),

    EVENT_DEADLINE_PASSED(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "모집이 마감되었습니다.",
        message = "모집 마감 시각이 지나\n더 이상 신청할 수 없습니다.",
    ),

    // 23xx - Conflict
    EVENT_FULL(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "정원이 초과되었습니다.",
        message = "모임 정원이 가득 차\n더 이상 신청할 수 없습니다.",
    ),
}
