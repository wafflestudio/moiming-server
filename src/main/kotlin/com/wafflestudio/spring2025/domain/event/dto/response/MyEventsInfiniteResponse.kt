package com.wafflestudio.spring2025.domain.event.dto.response

import java.time.Instant

data class MyEventsInfiniteResponse(
    val events: List<MyEventResponse>,
    val nextCursor: Instant?,  // 다음 요청용
    val hasNext: Boolean,
)

data class MyEventResponse(
    val publicId: String,
    val title: String,
    val startAt: Instant?,
    val endAt: Instant?,
    val registrationStart: Instant?,
    val registrationDeadline: Instant?,
    val capacity: Int?,
    val registrationCnt: Int,
)
