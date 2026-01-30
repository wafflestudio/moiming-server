package com.wafflestudio.spring2025.domain.registration.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationRequest
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationGuestsResponse
import com.wafflestudio.spring2025.domain.registration.service.RegistrationService
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events/{eventId}/registrations")
@Tag(name = "Registration", description = "이벤트 신청 관리 API")
class RegistrationController(
    private val registrationService: RegistrationService,
) {
    @Operation(
        summary = "참여자 신청",
        description =
            "이벤트에 신청합니다. 정원이 남아있으면 CONFIRMED로 등록되고, 정원이 찼으나 대기 명단이 허용된 경우 WAITING으로 등록됩니다. " +
                "정원이 찼고 대기 명단이 비활성화된 경우 신청이 거절됩니다.",
    )
    @PostMapping
    fun create(
        @PathVariable eventId: Long,
        @RequestBody request: CreateRegistrationRequest,
        @LoggedInUser user: User?,
    ): ResponseEntity<CreateRegistrationResponse> {
        val registration =
            registrationService.create(
                userId = user?.id,
                eventId = eventId,
                guestName = request.guestName,
                guestEmail = request.guestEmail,
            )
        return ResponseEntity.ok(registration)
    }

    @Operation(
        summary = "이벤트 신청 목록 조회",
        description = "이벤트 신청 목록을 조회합니다. 신청 상태(CONFIRMED/WAITING/CANCELED)와 신청 시각을 포함해 반환합니다.",
    )
    @GetMapping
    fun list(
        @PathVariable eventId: Long,
        @LoggedInUser user: User,
    ): ResponseEntity<RegistrationGuestsResponse> {
        val userId = requireNotNull(user.id) { "로그인 사용자 ID가 없습니다." }
        return ResponseEntity.ok(registrationService.getGuestsByEventId(eventId, userId))
    }

    // 나중에 프론트에 요청에 따라 수정
//    @Operation(
//        summary = "이벤트 신청 단건 조회",
//        description = "이벤트 신청 단건을 조회합니다.",
//    )
//    @GetMapping("/{registrationPublicId}")
//    fun get(
//        @PathVariable eventId: Long,
//        @PathVariable registrationPublicId: String,
//        @LoggedInUser user: User,
//    ): ResponseEntity<RegistrationDto> {
//        val userId = requireNotNull(user.id) { "로그인 사용자 ID가 없습니다." }
//        return ResponseEntity.ok(registrationService.getByPublicId(eventId, registrationPublicId, userId))
//    }
//
//    @Operation(
//        summary = "이벤트 신청 취소",
//        description = "취소 토큰을 이용해 신청을 취소합니다. 토큰은 24시간 내 만료되며, 취소 후 대기자가 자동 승격될 수 있습니다.",
//    )
//    @PostMapping("/{registrationPublicId}/cancel")
//    fun cancel(
//        @PathVariable eventId: Long,
//        @PathVariable registrationPublicId: String,
//        @RequestParam token: String,
//    ): ResponseEntity<Unit> {
//        registrationService.cancelWithTokenByPublicId(eventId, registrationPublicId, token)
//        return ResponseEntity.ok().build()
//    }
}
