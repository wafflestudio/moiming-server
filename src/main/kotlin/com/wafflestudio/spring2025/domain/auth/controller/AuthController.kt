package com.wafflestudio.spring2025.domain.auth.controller

import com.wafflestudio.spring2025.domain.auth.dto.LoginRequest
import com.wafflestudio.spring2025.domain.auth.dto.LoginResponse
import com.wafflestudio.spring2025.domain.auth.dto.LogoutResponse
import com.wafflestudio.spring2025.domain.auth.dto.SignupRequest
import com.wafflestudio.spring2025.domain.auth.service.AuthService
import com.wafflestudio.spring2025.domain.auth.service.EmailVerificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 API")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService,
) {
    @Operation(
        summary = "이메일 회원가입",
        description = "새로운 사용자를 등록하고 인증 이메일을 발송합니다. 이메일 인증 후 가입이 완료됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "인증 이메일 발송 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 email/password/name)"),
            ApiResponse(responseCode = "409", description = "이미 존재하는 email"),
            ApiResponse(responseCode = "503", description = "이메일 서비스 장애"),
        ],
    )
    @PostMapping("/signup")
    fun signup(
        @RequestBody signupRequest: SignupRequest,
    ): ResponseEntity<Void> {
        // profileImage는 회원가입에서 받지 않는 정책이면 null로 고정

        emailVerificationService.createPendingUser(
            email = signupRequest.email,
            name = signupRequest.name,
            password = signupRequest.password,
        )

        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "이메일 인증",
        description = "이메일로 받은 인증 코드를 검증하고 회원가입을 완료합니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "인증 성공, 유저 등록 완료"),
            ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 인증 코드"),
        ],
    )
    @PostMapping("/email-verification/{verificationCode}")
    fun verifyEmail(
        @PathVariable verificationCode: String,
    ): ResponseEntity<Void> {
        emailVerificationService.verifyEmailAndCreateUser(verificationCode)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "로그인", description = "email로 로그인하여 JWT 토큰을 발급받습니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
            ApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        val token = authService.login(loginRequest.email, loginRequest.password)
        return ResponseEntity.ok(LoginResponse(token))
    }

    @Operation(summary = "로그아웃", description = "현재 JWT 토큰을 블랙리스트에 추가하여 로그아웃합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "로그아웃 성공"),
        ],
    )
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<LogoutResponse> {
        val token = resolveToken(request)
        authService.logout(token)
        return ResponseEntity.noContent().build()
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
