package com.wafflestudio.spring2025.domain.registration.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 상태 변경")
data class PatchRegistrationResponse(
    val patchEmail: String? = null,
)
