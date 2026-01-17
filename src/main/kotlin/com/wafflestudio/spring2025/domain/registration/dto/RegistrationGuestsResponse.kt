package com.wafflestudio.spring2025.domain.registration.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 참여자 명단 응답")
data class RegistrationGuestsResponse(
    @Schema(description = "참여자 목록")
    val guests: List<Guest>,
) {
    @Schema(description = "참여자 정보")
    data class Guest(
        @Schema(description = "신청 ID")
        val id: Long,
        @Schema(description = "이름")
        val name: String,
        @Schema(description = "이메일 (관리자 요청 시 노출)")
        val email: String?,
        @JsonProperty("profile_image")
        @Schema(description = "프로필 이미지 URL")
        val profileImage: String?,
    )
}
