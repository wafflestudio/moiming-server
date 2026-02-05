package com.wafflestudio.spring2025.domain.event.exception

import com.wafflestudio.spring2025.common.exception.DomainException

open class EventException(
    error: EventErrorCode,
    cause: Throwable? = null,
) : DomainException(
    httpErrorCode = error.httpStatusCode,
    code = error,
    title = error.title,
    msg = error.message,
    cause = cause,
)

class EventNotFoundException : EventException(error = EventErrorCode.EVENT_NOT_FOUND)

class EventForbiddenException : EventException(error = EventErrorCode.NOT_EVENT_ADMIN)

class EventFullException : EventException(error = EventErrorCode.EVENT_FULL)

class EventDeadlinePassedException : EventException(error = EventErrorCode.EVENT_DEADLINE_PASSED)

class EventValidationException(
    error: EventErrorCode,
) : EventException(error = error)