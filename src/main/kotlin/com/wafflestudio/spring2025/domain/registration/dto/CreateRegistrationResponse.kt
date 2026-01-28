package com.wafflestudio.spring2025.domain.registration.dto

import com.wafflestudio.spring2025.domain.registration.dto.RegistrationStatusResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 응답")
data class CreateRegistrationResponse(
    @Schema(description = "신청 상태")
    val status: RegistrationStatusResponse,
    @Schema(description = "대기 순번 (대기 상태가 아니면 null)")
    val waitingNum: Int? = null,
    @Schema(description = "신청 확인 이메일")
    val confirmEmail: String? = null,
)
