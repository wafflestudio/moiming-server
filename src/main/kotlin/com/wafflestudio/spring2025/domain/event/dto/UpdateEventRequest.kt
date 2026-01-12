package com.wafflestudio.spring2025.domain.event.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일정 수정 요청")
data class UpdateEventRequest(
    @Schema(description = "이벤트 제목")
    val title: String?,

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
    val waitlistEnabled: Boolean?,

    @Schema(description = "신청 마감 시간 (epoch milliseconds)")
    val registrationDeadline: Long?,
)
