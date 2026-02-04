package com.wafflestudio.spring2025.domain.registration.dto

import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 정보 요청 응답")
data class GetRegistrationResponse(
    val status: RegistrationStatus,
    val waitlistPosition: Int,
    val registrationPublicId: String,
    val reservationEmail: String,
)
