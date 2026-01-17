package com.wafflestudio.spring2025.domain.event.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "일정 상세 조회 응답")
data class EventDetailResponse(
    @Schema(description = "이벤트 제목", example = "제2회 기획 세미나")
    val title: String,
    @Schema(description = "이벤트 설명", example = "일정 상세 설명...")
    val description: String?,
    @Schema(description = "장소", example = "서울대")
    val location: String?,
    @Schema(
        description = "시작 시간 (ISO-8601)",
        example = "2026-02-02T18:00:00Z",
    )
    val startAt: Instant?,
    @Schema(
        description = "종료 시간 (ISO-8601)",
        example = "2026-02-02T20:00:00Z",
    )
    val endAt: Instant?,
    @Schema(description = "정원", example = "10")
    val capacity: Int?,
    @Schema(description = "현재 참여 인원", example = "8")
    val currentParticipants: Int,
    @Schema(
        description = "신청 시작 시간 (ISO-8601)",
        example = "2026-02-01T17:00:00Z",
    )
    val registrationStart: Instant?,
    @Schema(
        description = "신청 마감 시간 (ISO-8601)",
        example = "2026-02-02T17:00:00Z",
    )
    val registrationDeadline: Instant?,
    @Schema(
        description = "요청자 역할",
        example = "CREATOR",
    )
    val myRole: MyRole,
    @Schema(
        description = "대기 순번 (대기 중이면 숫자, 아니면 null)",
        example = "3",
        nullable = true,
    )
    val waitingNum: Int?,
    @Schema(description = "참여자 미리보기 목록")
    val guestsPreview: List<GuestPreview>,
)

/**
 * 요청자 역할
 */
enum class MyRole {
    CREATOR,
    PARTICIPANT,
    NONE,
}

/**
 * 참여자 미리보기 정보
 */
data class GuestPreview(
    @Schema(description = "참여자 ID", example = "1")
    val id: Long,
    @Schema(description = "참여자 이름", example = "홍길동")
    val name: String,
    @Schema(description = "프로필 이미지 URL", example = "https://...")
    val profileImage: String?,
)
