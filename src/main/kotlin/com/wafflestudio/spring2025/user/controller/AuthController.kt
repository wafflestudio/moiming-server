package com.wafflestudio.spring2025.user.controller

import com.wafflestudio.spring2025.user.dto.LoginRequest
import com.wafflestudio.spring2025.user.dto.LoginResponse
import com.wafflestudio.spring2025.user.dto.LogoutResponse
import com.wafflestudio.spring2025.user.dto.RegisterRequest
import com.wafflestudio.spring2025.user.dto.RegisterResponse
import com.wafflestudio.spring2025.user.dto.SocialLoginRequest
import com.wafflestudio.spring2025.user.service.JwtBlacklistService
import com.wafflestudio.spring2025.user.service.OAuthService
import com.wafflestudio.spring2025.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 API")
class AuthController(
    private val userService: UserService,
    private val oauthService: OAuthService,
    private val jwtBlacklistService: JwtBlacklistService,
) {
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "회원가입 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (username 또는 password가 4자 미만)"),
            ApiResponse(responseCode = "409", description = "이미 존재하는 username"),
        ],
    )
    @PostMapping("/register")
    fun register(
        @RequestBody registerRequest: RegisterRequest,
    ): ResponseEntity<RegisterResponse> {
        val userDto =
            userService.register(
                username = registerRequest.username,
                password = registerRequest.password,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto)
    }

    @Operation(summary = "로그인", description = "username과 password로 로그인하여 JWT 토큰을 발급받습니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
            ApiResponse(responseCode = "401", description = "인증 실패 (username 또는 password 불일치)"),
        ],
    )
    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        val token =
            userService.login(
                username = loginRequest.username,
                password = loginRequest.password,
            )
        return ResponseEntity.status(HttpStatus.OK).body(LoginResponse(token))
    }

    @Operation(summary = "소셜 로그인", description = "구글, 카카오 등 소셜 로그인으로 JWT 토큰을 발급받습니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
            ApiResponse(responseCode = "401", description = "인증 실패"), // FIXME 이게 필요한 상황이 있나
            ApiResponse(responseCode = "404", description = "등록되지 않은 사용자") // 회원가입으로 넘어가야 함
        ],
    )
    @PostMapping("/social")
    fun social(
        @RequestBody socialLoginRequest: SocialLoginRequest,
    ): ResponseEntity<LoginResponse> {
        val token = oauthService.authenticate( // OAuth access token 아님! 우리 서비스 토큰
            provider = socialLoginRequest.provider,
            authCode = socialLoginRequest.code,
        )
        return ResponseEntity.status(HttpStatus.OK).body(LoginResponse(token))
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
        val token = resolveToken(request)
        if (token != null) {
            jwtBlacklistService.addToBlacklist(token)
        }
        return ResponseEntity.ok(LogoutResponse("Successfully logged out"))
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        return null
    }
}
