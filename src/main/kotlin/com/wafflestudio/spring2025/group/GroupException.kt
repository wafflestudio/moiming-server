package com.wafflestudio.spring2025.group

import com.wafflestudio.spring2025.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

sealed class GroupException(
    errorCode: Int,
    httpStatusCode: HttpStatusCode,
    msg: String,
    cause: Throwable? = null,
) : DomainException(errorCode, httpStatusCode, msg, cause)

class GroupNotFoundException : GroupException(
    errorCode = 0,
    httpStatusCode = HttpStatus.NOT_FOUND,
    msg = "Group not found",
)

class GroupNameConflictException : GroupException(
    errorCode = 0,
    httpStatusCode = HttpStatus.CONFLICT,
    msg = "Group name already exists",
)

class CreateBadGroupNameException : GroupException(
    errorCode = 0,
    httpStatusCode = HttpStatus.BAD_REQUEST,
    msg = "Bad groupname",
)

class GroupCodeConflictException : GroupException(
    errorCode = 0,
    httpStatusCode = HttpStatus.CONFLICT,
    msg = "Group code already exists",
)

class InvalidGroupCodeException : GroupException(
    errorCode = 0,
    httpStatusCode = HttpStatus.BAD_REQUEST,
    msg = "Invalid group code",
)
