package com.wafflestudio.spring2025.domain.registration.dto

import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "이벤트 신청 목록 조회 응답")
data class GetEventRegistrationsResponse(
    @Schema(description = "신청자 목록")
    val participants: List<EventRegistrationItem>,
    @Schema(description = "필터에 따라 변하는 참여자 수")
    val totalCount: Int?,
    @Schema(description = "다음 페이지 커서 (없으면 null)")
    val nextCursor: Int?,
    @Schema(description = "다음 페이지 존재 여부")
    val hasNext: Boolean,
)

@Schema(description = "신청자 정보")
data class EventRegistrationItem(
    @Schema(description = "신청 공개 ID")
    val registrationId: String,
    @Schema(description = "이름")
    val name: String,
    @Schema(description = "이메일 (관리자 요청 시 노출)")
    val email: String?,
    @Schema(description = "프로필 이미지 URL")
    val profileImage: String?,
    @Schema(description = "신청 일시")
    val createdAt: Instant,
    @Schema(description = "신청 상태")
    val status: RegistrationStatus,
    @Schema(description = "대기 순번 (status가 WAITLISTED일 때만 아니면 null)")
    val waitingNum: Int?,
) {
    constructor(
        registration: Registration,
        profileImage: String?,
        user: User?,
        waitingNum: Int?,
    ) : this(
        registrationId = registration.registrationPublicId,
        name = registration.guestName ?: user?.name.orEmpty(),
        email = registration.guestEmail ?: user?.email,
        profileImage = profileImage,
        createdAt = registration.createdAt!!,
        status = registration.status,
        waitingNum = waitingNum,
    )
}
