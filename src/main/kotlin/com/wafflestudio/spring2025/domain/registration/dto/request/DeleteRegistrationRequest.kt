package com.wafflestudio.spring2025.domain.registration.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 취소 요청")
data class DeleteRegistrationRequest(
    @Schema(description = "비회원 이름")
    val guestName: String? = null,
    @Schema(description = "비회원 이메일")
    val guestEmail: String? = null,
)
