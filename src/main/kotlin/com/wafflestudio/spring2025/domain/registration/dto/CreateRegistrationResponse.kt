package com.wafflestudio.spring2025.domain.registration.dto

import com.wafflestudio.spring2025.domain.registration.dto.core.RegistrationDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 응답")
data class CreateRegistrationResponse(
    @Schema(description = "신청 정보")
    val registration: RegistrationDto,
    @Schema(description = "신청 취소 토큰")
    val cancelToken: String,
)
