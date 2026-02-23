package com.wafflestudio.spring2025.domain.registration.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.registration.dto.request.CreateRegistrationRequest
import com.wafflestudio.spring2025.domain.registration.dto.response.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.response.GetEventRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.service.RegistrationService
import com.wafflestudio.spring2025.domain.registration.service.command.CreateRegistrationCommand
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "신청 성공"),
            ApiResponse(
                responseCode = "400",
                description =
                    "유효하지 않은 요청: 3003(Wrong guest name), 3004(Wrong guest email), 2204(Invalid registration window)",
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음: 3006(Banned user cannot register)",
            ),
            ApiResponse(responseCode = "404", description = "이벤트 없음: 2001(Event not found)"),
            ApiResponse(
                responseCode = "409",
                description = "신청 불가: 3001(Registration already exists), 3005(Host cannot register), 2301(Event is full)",
            ),
        ],
    )
    @PostMapping
    fun create(
        @PathVariable eventId: String,
        @RequestBody request: CreateRegistrationRequest,
        @LoggedInUser user: User?,
    ): ResponseEntity<CreateRegistrationResponse> {
        val command =
            if (user != null) {
                CreateRegistrationCommand.Member(
                    userId = user.id!!,
                    eventId = eventId,
                )
            } else {
                CreateRegistrationCommand.Guest(
                    eventId = eventId,
                    name = requireNotNull(request.guestName) { "guestName is required" },
                    email = requireNotNull(request.guestEmail) { "guestEmail is required" },
                )
            }

        val registration = registrationService.create(command)
        return ResponseEntity.ok(registration)
    }

    @Operation(
        summary = "이벤트 신청 목록 조회",
        description = "특정 이벤트에 속한 신청 목록을 조회합니다. 신청 상태(CONFIRMED/WAITLISTED/BANNED)와 신청 시각을 포함해 반환합니다.",
    )
    @GetMapping
    fun list(
        @PathVariable eventId: String,
        @LoggedInUser user: User?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) orderBy: String?,
        @Parameter(description = "다음 페이지 시작 offset (없으면 0부터)")
        @RequestParam(required = false) cursor: Int?,
    ): ResponseEntity<GetEventRegistrationsResponse> =
        ResponseEntity.ok(
            registrationService.getEventRegistration(
                eventId = eventId,
                requesterId = user?.id,
                status = status,
                orderBy = orderBy,
                cursor = cursor,
            ),
        )
}
