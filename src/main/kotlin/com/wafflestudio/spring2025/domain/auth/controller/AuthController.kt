package com.wafflestudio.spring2025.domain.auth.controller

import com.wafflestudio.spring2025.domain.auth.dto.LoginRequest
import com.wafflestudio.spring2025.domain.auth.dto.LoginResponse
import com.wafflestudio.spring2025.domain.auth.dto.LogoutResponse
import com.wafflestudio.spring2025.domain.auth.dto.RegisterRequest
import com.wafflestudio.spring2025.domain.auth.dto.RegisterResponse
import com.wafflestudio.spring2025.domain.auth.service.JwtBlacklistService
import com.wafflestudio.spring2025.domain.auth.service.UserService
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
    private val jwtBlacklistService: JwtBlacklistService,
) {
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "회원가입 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (email 누락)"),
            ApiResponse(responseCode = "409", description = "이미 존재하는 email"),
        ],
    )
    @PostMapping("/register")
    fun register(
        @RequestBody registerRequest: RegisterRequest,
    ): ResponseEntity<RegisterResponse> {
        TODO("회원가입 API 구현")
    }

    @Operation(summary = "로그인", description = "email로 로그인하여 JWT 토큰을 발급받습니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "로그인 성공, JWT 토큰 반환"),
            ApiResponse(responseCode = "401", description = "인증 실패 (email 불일치)"),
        ],
    )
    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        TODO("로그인 API 구현")
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
