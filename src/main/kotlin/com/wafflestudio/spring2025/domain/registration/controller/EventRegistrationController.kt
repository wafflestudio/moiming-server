package com.wafflestudio.spring2025.domain.registration.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationRequest
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.GetEventRegistrationsResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events/{eventId}/registrations")
@Tag(name = "Registration", description = "이벤트 기준 신청 API")
class EventRegistrationController(
    private val registrationService: RegistrationService,
) {
    @Operation(
        summary = "이벤트 참여 신청",
        description =
            "특정 이벤트에 신청합니다. 정원이 남아있으면 CONFIRMED로 등록되고, 정원이 찼으나 대기 명단이 허용된 경우 WAITLISTED로 등록됩니다. " +
                "정원이 찼고 대기 명단이 비활성화된 경우 신청이 거절됩니다.",
    )
    @PostMapping
    fun create(
        @PathVariable eventId: String,
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
        description = "특정 이벤트에 속한 신청 목록을 조회합니다. 신청 상태(CONFIRMED/WAITLISTED/CANCELED)와 신청 시각을 포함해 반환합니다.",
    )
    @GetMapping
    fun list(
        @PathVariable eventId: String,
        @LoggedInUser user: User,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) orderBy: String?,
        @RequestParam(required = false) cursor: Int?,
    ): ResponseEntity<GetEventRegistrationsResponse> {
        val userId = requireNotNull(user.id) { "로그인 사용자 ID가 없습니다." }
        return ResponseEntity.ok(
            registrationService.getEventRegistration(
                eventId = eventId,
                requesterId = userId,
                status = status,
                orderBy = orderBy,
                cursor = cursor,
            ),
        )
    }
}
