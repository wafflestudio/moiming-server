package com.wafflestudio.spring2025.domain.user.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.auth.service.AuthService
import com.wafflestudio.spring2025.domain.user.dto.GetMeResponse
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
class UserController(
    private val authService: AuthService,
    private val userService: UserService,
) {
    @Operation(summary = "본인 정보 조회", description = "로그인한 사용자의 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @GetMapping("/me")
    fun me(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ResponseEntity<GetMeResponse> {
        return ResponseEntity.ok(userService.me(user))
    }

    @Operation(summary = "로그아웃", description = "현재 JWT 토큰을 무효화합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            ApiResponse(responseCode = "401", description = "인증 실패"),
        ],
    )
    @PostMapping("/logout")
    fun logout(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @Parameter(hidden = true) @org.springframework.web.bind.annotation.RequestHeader("Authorization") authorization: String,
    ): ResponseEntity<Unit> {
        TODO("로그아웃 API 구현")
    }
}
