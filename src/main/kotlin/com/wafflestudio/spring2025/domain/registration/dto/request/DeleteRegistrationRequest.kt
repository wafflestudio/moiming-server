package com.wafflestudio.spring2025.domain.registration.dto.request

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 취소 요청")
data class DeleteRegistrationRequest(
    @Schema(description = "비회원 이름")
    @JsonAlias("guest_name")
    val guestName: String? = null,
    @Schema(description = "비회원 이메일")
    @JsonAlias("guest_email")
    val guestEmail: String? = null,
)
