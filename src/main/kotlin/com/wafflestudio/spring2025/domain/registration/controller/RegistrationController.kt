package com.wafflestudio.spring2025.domain.registration.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.registration.RegistrationInvalidStatusException
import com.wafflestudio.spring2025.domain.registration.dto.PatchRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.UpdateRegistrationStatusRequest
import com.wafflestudio.spring2025.domain.registration.service.RegistrationService
import com.wafflestudio.spring2025.domain.user.identity.UserIdentityNotFoundException
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/registrations/{registrationId}")
@Tag(name = "Registration", description = "신청 단건 리소스 API")
class RegistrationController(
    private val registrationService: RegistrationService,
) {

    @Operation(
        summary = "신청 단건 상태 변경",
        description =
            "신청 단건 리소스의 상태를 변경합니다. " +
                "사용자가 본인의 신청을 취소하는 경우와 관리자가 강제 취소/밴 처리하는 경우를 로직 레벨에서 구분합니다.",
    )
    @PatchMapping
    fun updateStatus(
        @PathVariable registrationId: Long,
        @RequestBody request: UpdateRegistrationStatusRequest,
        @LoggedInUser user: User,
    ): ResponseEntity<PatchRegistrationResponse> {
        val userId = user.id ?: throw UserIdentityNotFoundException()
        val status = request.status ?: throw RegistrationInvalidStatusException()
        val response = registrationService.updateStatus(userId, registrationId, status)

        return ResponseEntity.ok(response)
    }
}
