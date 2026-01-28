package com.wafflestudio.spring2025.domain.registration.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.registration.RegistrationInvalidStatusException
import com.wafflestudio.spring2025.domain.registration.dto.PatchRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.UpdateRegistrationStatusRequest
import com.wafflestudio.spring2025.domain.registration.service.RegistrationService
import com.wafflestudio.spring2025.domain.user.identity.UserIdentityNotFoundException
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/registrations/{registrationId}")
@Tag(name = "", description = "이벤트 신청 관리자 API")
class RegistrationHostController(
    private val registrationService: RegistrationService,
) {
    @PatchMapping
    fun updateStatus(
        @PathVariable registrationId: Long,
        @RequestBody request: UpdateRegistrationStatusRequest,
        @LoggedInUser user: User,
    ): ResponseEntity<PatchRegistrationResponse> {
        val userId = user.id ?: throw UserIdentityNotFoundException()
        val status = request.status ?: throw RegistrationInvalidStatusException()
        val response = registrationService.updateStatusByHost(userId, registrationId, status)

        return ResponseEntity.ok(response)
    }
}
