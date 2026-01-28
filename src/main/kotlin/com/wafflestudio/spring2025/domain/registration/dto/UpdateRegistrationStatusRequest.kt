package com.wafflestudio.spring2025.domain.registration.dto

import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 상태 변경 요청")
data class UpdateRegistrationStatusRequest(
    val status: RegistrationStatus? = null,
)
