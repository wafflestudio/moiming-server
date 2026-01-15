package com.wafflestudio.spring2025.domain.event.dto.core

import com.wafflestudio.spring2025.domain.event.model.Event
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일정 정보")
data class EventDto(
    @Schema(description = "일정 ID")
    val id: Long,
    @Schema(description = "이벤트 제목")
    val title: String,
    @Schema(description = "이벤트 설명")
    val description: String?,
    @Schema(description = "장소")
    val location: String?,
    @Schema(description = "시작 시간 (epoch milliseconds)")
    val startAt: Long?,
    @Schema(description = "종료 시간 (epoch milliseconds)")
    val endAt: Long?,
    @Schema(description = "정원")
    val capacity: Int?,
    @Schema(description = "대기 명단 사용 여부")
    val waitlistEnabled: Boolean,
    @Schema(description = "신청 마감 시간 (epoch milliseconds)")
    val registrationDeadline: Long?,
    @Schema(description = "작성자 ID")
    val createdBy: Long,
    @Schema(description = "생성 일시 (epoch milliseconds)")
    val createdAt: Long?,
    @Schema(description = "수정 일시 (epoch milliseconds)")
    val updatedAt: Long?,
) {
    constructor(event: Event) : this(
        id = requireNotNull(event.id) { "Event id is null" },
        title = event.title,
        description = event.description,
        location = event.location,
        startAt = event.startAt?.toEpochMilli(),
        endAt = event.endAt?.toEpochMilli(),
        capacity = event.capacity,
        waitlistEnabled = event.waitlistEnabled,
        registrationDeadline = event.registrationDeadline?.toEpochMilli(),
        createdBy = event.createdBy,
        createdAt = event.createdAt?.toEpochMilli(),
        updatedAt = event.updatedAt?.toEpochMilli(),
    )
}
