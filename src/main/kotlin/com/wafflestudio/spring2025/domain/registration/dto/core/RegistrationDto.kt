package com.wafflestudio.spring2025.domain.registration.dto.core

import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이벤트 신청 정보")
data class RegistrationDto(
    @Schema(description = "신청 ID")
    val id: Long,

    @Schema(description = "사용자 ID")
    val userId: Long?,

    @Schema(description = "이벤트 ID")
    val eventId: Long,

    @Schema(description = "비회원 이름")
    val guestName: String?,

    @Schema(description = "비회원 이메일")
    val guestEmail: String?,

    @Schema(description = "신청 상태")
    val status: RegistrationStatus,

    @Schema(description = "신청 일시 (epoch milliseconds)")
    val createdAt: Long,
) {
    constructor(registration: Registration) : this(
        id = TODO("Registration -> RegistrationDto 매핑 구현"),
        userId = TODO("Registration -> RegistrationDto 매핑 구현"),
        eventId = TODO("Registration -> RegistrationDto 매핑 구현"),
        guestName = TODO("Registration -> RegistrationDto 매핑 구현"),
        guestEmail = TODO("Registration -> RegistrationDto 매핑 구현"),
        status = TODO("Registration -> RegistrationDto 매핑 구현"),
        createdAt = TODO("Registration -> RegistrationDto 매핑 구현"),
    )
}
