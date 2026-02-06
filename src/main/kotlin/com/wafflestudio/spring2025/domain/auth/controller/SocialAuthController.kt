package com.wafflestudio.spring2025.domain.auth.controller

import com.wafflestudio.spring2025.domain.auth.dto.LoginResponse
import com.wafflestudio.spring2025.domain.auth.dto.SocialLoginRequest
import com.wafflestudio.spring2025.domain.auth.model.SocialProvider
import com.wafflestudio.spring2025.domain.auth.service.SocialAuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/auth/social")
class SocialAuthController(
    private val socialAuthService: SocialAuthService,
) {
    @Operation(summary = "소셜 로그인", description = "소셜 로그인을 요청합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "소셜 로그인 성공"),
            ApiResponse(responseCode = "401", description = "인증 실패"),
            ApiResponse(responseCode = "409", description = "이미 가입된 이메일"),
        ],
    )
    @PostMapping
    suspend fun social(
        @RequestBody socialLoginRequest: SocialLoginRequest,
    ): ResponseEntity<LoginResponse> {
        val response =
            socialAuthService.socialLogin(
                provider = SocialProvider.valueOf(socialLoginRequest.provider),
                code = socialLoginRequest.code,
            )
        return ResponseEntity.ok(response)
    }
}
