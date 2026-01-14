package com.wafflestudio.spring2025.domain.registration.dto.core

import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.registration.model.Registration
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 이벤트 신청 정보")
data class RegistrationWithEventDto(
    @Schema(description = "신청 정보")
    val registration: RegistrationDto,
    @Schema(description = "이벤트 요약 정보")
    val event: EventSummaryDto,
) {
    constructor(registration: Registration, event: Event) : this(
        registration = RegistrationDto(registration),
        event = EventSummaryDto(event),
    )
}

@Schema(description = "이벤트 요약 정보")
data class EventSummaryDto(
    @Schema(description = "이벤트 ID")
    val id: Long,
    @Schema(description = "이벤트 제목")
    val title: String,
    @Schema(description = "장소")
    val location: String?,
    @Schema(description = "시작 시간 (epoch milliseconds)")
    val startAt: Long?,
    @Schema(description = "종료 시간 (epoch milliseconds)")
    val endAt: Long?,
) {
    constructor(event: Event) : this(
        id = event.id!!,
        title = event.title,
        location = event.location,
        startAt = event.startAt?.toEpochMilli(),
        endAt = event.endAt?.toEpochMilli(),
    )
}
