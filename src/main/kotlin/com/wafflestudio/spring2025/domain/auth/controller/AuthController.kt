package com.wafflestudio.spring2025.domain.auth.controller

import com.wafflestudio.spring2025.config.EmailConfig
import com.wafflestudio.spring2025.domain.auth.dto.LoginRequest
import com.wafflestudio.spring2025.domain.auth.dto.LoginResponse
import com.wafflestudio.spring2025.domain.auth.dto.LogoutResponse
import com.wafflestudio.spring2025.domain.auth.dto.SignupRequest
import com.wafflestudio.spring2025.domain.auth.dto.SignupResponse
import com.wafflestudio.spring2025.domain.auth.service.AuthService
import com.wafflestudio.spring2025.domain.auth.service.JwtBlacklistService
import com.wafflestudio.spring2025.domain.user.InvalidVerificationCodeException
import com.wafflestudio.spring2025.domain.user.VerificationCodeExpiredException
import com.wafflestudio.spring2025.domain.user.service.EmailVerificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 API")
class AuthController(
    private val authService: AuthService,
    private val jwtBlacklistService: JwtBlacklistService,
    private val emailVerificationService: EmailVerificationService,
    private val emailConfig: EmailConfig,
) {
//    @Operation(
//        summary = "이메일 회원가입",
//        description = "새로운 사용자를 등록하고 인증 이메일을 발송합니다. 이메일 인증 후 가입이 완료됩니다.",
//    )
//    @ApiResponses(
//        value = [
//            ApiResponse(responseCode = "200", description = "회원가입 요청 성공, 인증 이메일 발송됨"),
//            ApiResponse(responseCode = "400", description = "잘못된 요청 (email 또는 password 누락)"),
//            ApiResponse(responseCode = "409", description = "이미 존재하는 email"),
//        ],
//    )
    @Operation(
        summary = "회원가입",
        description = "새로운 사용자를 등록합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "회원가입 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 email/password/name)"),
            ApiResponse(responseCode = "409", description = "이미 존재하는 email"),
        ],
    )
    @PostMapping("/signup")
    fun signup(
        @RequestBody signupRequest: SignupRequest,
//    ): ResponseEntity<Map<String, String>> {
    ): ResponseEntity<SignupResponse> {
//        val passwordHash = BCrypt.hashpw(signupRequest.password, BCrypt.gensalt())
//
//        emailVerificationService.createPendingUser(
//            email = signupRequest.email,
//            name = signupRequest.name,
//            passwordHash = passwordHash,
//        )
//
//        return ResponseEntity.ok(
//            mapOf("message" to "인증 이메일이 발송되었습니다. 이메일을 확인해주세요."),
//        )

        val userDto =
            authService.signup(
                email = signupRequest.email,
                name = signupRequest.name,
                password = signupRequest.password,
                profileImage = signupRequest.profileImage,
            )

        return ResponseEntity.status(HttpStatus.CREATED).body(userDto)
    }

    @Operation(
        summary = "이메일 인증",
        description = "이메일로 받은 인증 코드를 검증하고 회원가입을 완료합니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "302", description = "인증 성공, 프론트엔드로 리디렉션"),
            ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 인증 코드"),
        ],
    )
    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam code: String,
    ): ResponseEntity<Void> =
        try {
            emailVerificationService.verifyEmailAndCreateUser(code)
            // Redirect to frontend success page
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("${emailConfig.serviceDomain}/verification-result?status=success"))
                .build()
        } catch (e: VerificationCodeExpiredException) {
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("${emailConfig.serviceDomain}/verification-result?status=error&reason=expired"))
                .build()
        } catch (e: InvalidVerificationCodeException) {
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("${emailConfig.serviceDomain}/verification-result?status=error&reason=invalid"))
                .build()
        }

    @Operation(summary = "로그인", description = "email로 로그인하여 JWT 토큰을 발급받습니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
            ApiResponse(responseCode = "401", description = "인증 실패 (email 불일치)"),
        ],
    )
    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        val token =
            authService.login(
                loginRequest.email,
                loginRequest.password,
            )
        return ResponseEntity.ok(
            LoginResponse(token),
        )
    }

    @Operation(summary = "로그아웃", description = "현재 JWT 토큰을 블랙리스트에 추가하여 로그아웃합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<LogoutResponse> {
        TODO("로그아웃 API 구현")
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        TODO("Authorization 헤더에서 토큰 추출 구현")
    }
}
