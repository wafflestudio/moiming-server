package com.wafflestudio.spring2025.domain.user.controller

import com.wafflestudio.spring2025.domain.auth.AuthRequired
import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.user.dto.GetMeResponse
import com.wafflestudio.spring2025.domain.user.dto.PatchMeRequest
import com.wafflestudio.spring2025.domain.user.dto.PatchMeResponse
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@AuthRequired
@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 API")
class UserController(
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
    ): ResponseEntity<GetMeResponse> = ResponseEntity.ok(userService.me(user))

    @PatchMapping("/me")
    fun patchMe(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestBody request: PatchMeRequest,
    ): ResponseEntity<PatchMeResponse> {
        userService.patchMe(
            user = user,
            name = request.name,
            password = request.password,
            email = request.email,
            profileImage = request.profileImage,
        )
        return ResponseEntity.ok(userService.me(user))
    }

    @DeleteMapping("/me")
    fun deleteMe(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ResponseEntity<Void> {
        userService.deleteMe(user = user)
        return ResponseEntity.ok().build()
    }
}
