package com.wafflestudio.spring2025.domain.event.exception

import org.springframework.http.HttpStatus

/**
 * 2000 ~ 2999 : Event domain error codes
 * - 20xx: Not Found
 * - 21xx: Forbidden
 * - 22xx: Validation (Bad Request)
 * - 23xx: Conflict
 */
enum class EventErrorCode(
    val code: Int,
    val httpStatus: HttpStatus,
    val defaultMessage: String,
) {
    // 20xx - Not Found
    EVENT_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "Event not found"),

    // 21xx - Forbidden
    EVENT_FORBIDDEN(2101, HttpStatus.FORBIDDEN, "No permission for this event"),

    // 22xx - Validation / Bad Request
    EVENT_TITLE_BLANK(2201, HttpStatus.BAD_REQUEST, "Title must not be blank"),
    EVENT_TIME_RANGE_INVALID(2202, HttpStatus.BAD_REQUEST, "Invalid event time range"),
    EVENT_CAPACITY_INVALID(2203, HttpStatus.BAD_REQUEST, "Capacity must be positive"),
    EVENT_REGISTRATION_WINDOW_INVALID(
        2204,
        HttpStatus.BAD_REQUEST,
        "Invalid registration window",
    ),
    EVENT_DEADLINE_PASSED(
        2205,
        HttpStatus.BAD_REQUEST,
        "Event registration deadline has passed",
    ),

    // 23xx - Conflict
    EVENT_FULL(
        2301,
        HttpStatus.CONFLICT,
        "Event is full",
    ),
}
