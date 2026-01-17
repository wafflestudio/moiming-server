package com.wafflestudio.spring2025.domain.registration.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 응답")
data class CreateRegistrationResponse(
    @JsonProperty("waiting_num")
    @Schema(description = "대기 순번 (대기 상태가 아니면 null)")
    val waitingNum: String? = null,
)
