package com.wafflestudio.spring2025.domain.event.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "일정 상세 조회 응답")
data class EventDetailResponse(
    @Schema(description = "이벤트 정보")
    val event: EventInfo,
    @Schema(description = "이벤트 생성자 정보")
    val creator: CreatorInfo,
    @Schema(description = "조회자(viewer) 정보")
    val viewer: ViewerInfo,
    @Schema(description = "조회자가 할 수 있는 동작")
    val capabilities: CapabilitiesInfo,
    @Schema(description = "참여자 미리보기 목록")
    val guestsPreview: List<GuestPreview>,
)

@Schema(description = "이벤트 정보 블록")
data class EventInfo(
    @Schema(description = "이벤트 publicId", example = "dj8sadj10djnkas")
    val publicId: String,
    @Schema(description = "이벤트 제목", example = "제2회 기획 세미나")
    val title: String,
    @Schema(description = "이벤트 설명", example = "일정 상세 설명...", nullable = true)
    val description: String?,
    @Schema(description = "장소", example = "서울대", nullable = true)
    val location: String?,

    @Schema(description = "시작 시간 (ISO-8601)", example = "2026-02-02T18:00:00Z", nullable = true)
    val startsAt: Instant?,

    @Schema(description = "종료 시간 (ISO-8601)", example = "2026-02-02T20:00:00Z", nullable = true)
    val endsAt: Instant?,

    @Schema(description = "정원", example = "10", nullable = true)
    val capacity: Int?,
    @Schema(description = "총 신청자 수(확정+대기 등)", example = "8")
    val totalApplicants: Int,

    @Schema(description = "신청 시작 시간 (ISO-8601)", example = "2026-02-02T17:00:00Z", nullable = true)
    val registrationStartsAt: Instant?,

    @Schema(description = "신청 마감 시간 (ISO-8601)", example = "2026-02-02T17:00:00Z", nullable = true)
    val registrationEndsAt: Instant?,
)

@Schema(description = "생성자 정보 블록")
data class CreatorInfo(
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @Schema(description = "프로필 이미지", example = "https://example.com/profile.png", nullable = true)
    val profileImage: String?,
)

@Schema(description = "조회자(viewer) 정보 블록")
data class ViewerInfo(
    @Schema(
        description = "viewer의 등록 상태 (RegistrationStatus 사용)",
        example = "WAITLISTED",
    )
    val status: ViewerStatus,
    @Schema(
        description = "대기 순번 (WAITING일 때만 숫자, 아니면 null)",
        example = "3",
        nullable = true,
    )
    val waitlistPosition: Int?,
    @Schema(
        description = "registrationPublicId (게스트면 값, 아니면 null)",
        example = "1",
        nullable = true,
    )
    val registrationPublicId: String?,
    @Schema(
        description = "예약 이메일(게스트면 guestEmail, 로그인 유저면 user email 등 정책에 따름)",
        example = "skdfsj@gmaldkl",
        nullable = true,
    )
    val reservationEmail: String?,
)

@Schema(description = "가능한 동작 블록")
data class CapabilitiesInfo(
    @Schema(description = "공유 링크 가능 여부", example = "false")
    val shareLink: Boolean,

    @Schema(description = "참여 신청 가능 여부(확정 자리 신청)", example = "false")
    val apply: Boolean,

    @Schema(description = "대기 신청 가능 여부(정원 찼을 때)", example = "false")
    val wait: Boolean,

    @Schema(description = "신청 취소 가능 여부", example = "true")
    val cancel: Boolean,
)

