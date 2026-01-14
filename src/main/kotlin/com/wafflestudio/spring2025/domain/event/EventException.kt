package com.wafflestudio.spring2025.domain.event

import com.wafflestudio.spring2025.common.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class EventException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class EventNotFoundException :
    EventException(
        errorCode = 0,
        httpStatusCode = HttpStatus.NOT_FOUND,
        msg = "Event not found",
    )

class EventFullException :
    EventException(
        errorCode = 0,
        httpStatusCode = HttpStatus.CONFLICT,
        msg = "Event is full",
    )

class EventDeadlinePassedException :
    EventException(
        errorCode = 0,
        httpStatusCode = HttpStatus.BAD_REQUEST,
        msg = "Event registration deadline has passed",
    )

class EventUnauthorizedException :
    EventException(
        errorCode = 0,
        httpStatusCode = HttpStatus.FORBIDDEN,
        msg = "Not authorized to modify this event",
    )
