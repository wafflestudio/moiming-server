package com.wafflestudio.spring2025.domain.event.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일정 생성 요청")
data class CreateEventRequest(
    @Schema(description = "이벤트 제목", example = "정기 모임")
    val title: String,
    @Schema(description = "이벤트 설명", example = "정기 모임입니다")
    val description: String? = null,
    @Schema(description = "장소", example = "강의실 101")
    val location: String? = null,
    @Schema(description = "시작 시간 (epoch milliseconds)", example = "1700000000000")
    val startAt: Long? = null,
    @Schema(description = "종료 시간 (epoch milliseconds)", example = "1700003600000")
    val endAt: Long? = null,
    @Schema(description = "정원", example = "30")
    val capacity: Int? = null,
    @Schema(description = "대기 명단 사용 여부", example = "true")
    val waitlistEnabled: Boolean,
    @Schema(description = "신청 마감 시간 (epoch milliseconds)", example = "1699900000000")
    val registrationDeadline: Long? = null,
)
