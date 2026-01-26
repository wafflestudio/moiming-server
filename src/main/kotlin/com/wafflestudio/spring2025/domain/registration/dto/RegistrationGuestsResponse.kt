package com.wafflestudio.spring2025.domain.registration.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 참여자 명단 응답")
data class RegistrationGuestsResponse(
    @Schema(description = "확정 참여자 목록")
    val guests: List<Guest>,
    @JsonProperty("confirmed_count")
    @Schema(description = "확정 참여자 수")
    val confirmedCount: Int,
    @JsonProperty("waiting_count")
    @Schema(description = "대기 참여자 수")
    val waitingCount: Int,
) {
    @Schema(description = "참여자 정보")
    data class Guest(
        @Schema(description = "신청 공개 ID")
        val registrationPublicId: String,
        @Schema(description = "이름")
        val name: String,
        @Schema(description = "이메일 (관리자 요청 시 노출)")
        val email: String?,
        @JsonProperty("profile_image")
        @Schema(description = "프로필 이미지 URL")
        val profileImage: String?,
    )
}
