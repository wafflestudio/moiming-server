package com.wafflestudio.spring2025.domain.registration.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 요청")
data class CreateRegistrationRequest(
    @JsonProperty("guest_name")
    @Schema(description = "비회원 이름")
    val guestName: String? = null,
    @JsonProperty("guest_email")
    @Schema(description = "비회원 이메일")
    val guestEmail: String? = null,
)
