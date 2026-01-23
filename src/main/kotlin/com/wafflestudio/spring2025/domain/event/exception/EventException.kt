package com.wafflestudio.spring2025.domain.event.exception

import com.wafflestudio.spring2025.common.exception.DomainException

/**
 * Base exception for Event domain.
 * - Holds a domain-specific errorCode (2000~2999)
 * - Holds an HTTP status code
 * - Holds a human-readable message
 */
open class EventException(
    val error: EventErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : DomainException(
    errorCode = error.code,
    httpErrorCode = error.httpStatus,
    msg = message ?: error.defaultMessage,
    cause = cause,
)

/* =========================
 * Concrete exceptions
 * ========================= */

/**
 * Event not found (by publicId, etc.)
 */
class EventNotFoundException(
    publicId: String? = null,
    cause: Throwable? = null,
) : EventException(
    error = EventErrorCode.EVENT_NOT_FOUND,
    message = publicId?.let { "Event not found (publicId=$it)" },
    cause = cause,
)

/**
 * Only creator can modify/delete, etc.
 */
class EventForbiddenException(
    requesterId: Long? = null,
    cause: Throwable? = null,
) : EventException(
    error = EventErrorCode.EVENT_FORBIDDEN,
    message = requesterId?.let { "Not authorized to modify this event (requesterId=$it)" },
    cause = cause,
)

/**
 * Event is full (capacity reached)
 */
class EventFullException(
    cause: Throwable? = null,
) : EventException(
    error = EventErrorCode.EVENT_FULL,
    cause = cause,
)

/**
 * Registration deadline has passed
 */
class EventDeadlinePassedException(
    cause: Throwable? = null,
) : EventException(
    error = EventErrorCode.EVENT_DEADLINE_PASSED,
    cause = cause,
)

/**
 * Validation errors (pick one code depending on what failed)
 * - Use this when you want to throw with a specific validation code without creating many classes.
 *
 * NOTE: This class is restricted to 22xx error codes.
 */
class EventValidationException(
    error: EventErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : EventException(
    error = error,
    message = message,
    cause = cause,
) {
    init {
        require(error.code in 2200..2299) {
            "EventValidationException must use 22xx error codes. given=${error.code}"
        }
    }
}
