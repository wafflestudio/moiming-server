package com.wafflestudio.spring2025.domain.event.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "일정 수정 요청")
data class UpdateEventRequest(
    @Schema(description = "이벤트 제목", example = "정기 모임")
    val title: String? = null,
    @Schema(description = "이벤트 설명", example = "정기 모임입니다")
    val description: String? = null,
    @Schema(description = "장소", example = "강의실 101")
    val location: String? = null,
    @Schema(description = "시작 시간 (ISO-8601)", example = "2026-02-02T18:00:00Z")
    val startAt: Instant? = null,
    @Schema(description = "종료 시간 (ISO-8601)", example = "2026-02-02T20:00:00Z")
    val endAt: Instant? = null,
    @Schema(description = "정원", example = "30")
    val capacity: Int? = null,
    @Schema(description = "대기 명단 사용 여부", example = "true")
    val waitlistEnabled: Boolean? = null,
    @Schema(description = "신청 시작 시간 (ISO-8601)", example = "2026-02-02T17:00:00Z")
    val registrationStart: Instant? = null,
    @Schema(description = "신청 마감 시간 (ISO-8601)", example = "2026-02-02T17:30:00Z")
    val registrationDeadline: Instant? = null,
)
