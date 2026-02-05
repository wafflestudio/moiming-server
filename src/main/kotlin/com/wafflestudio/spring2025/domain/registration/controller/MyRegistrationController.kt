package com.wafflestudio.spring2025.domain.registration.controller

import com.wafflestudio.spring2025.domain.auth.AuthRequired
import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.registration.dto.GetMyRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.service.RegistrationService
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@AuthRequired
@RestController
@RequestMapping("/api/v1/registrations")
@Tag(name = "Registration", description = "내 신청 조회 API")
class MyRegistrationController(
    private val registrationService: RegistrationService,
) {
    @Operation(
        summary = "내 이벤트 신청 목록 조회",
        description = "로그인한 사용자의 이벤트 신청 목록을 조회합니다. 최신 신청 순으로 반환되며 page/size로 페이징합니다.",
    )
    @GetMapping("/me")
    fun myRegistrations(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<GetMyRegistrationsResponse> {
        val userId = requireNotNull(user.id) { "로그인 사용자 ID가 없습니다." }
        return ResponseEntity.ok(registrationService.getMyRegistrations(userId, page, size))
    }
}
