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
            ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - 로그인이 필요합니다"),
        ],
    )
    @GetMapping("/me")
    fun me(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ResponseEntity<GetMeResponse> = ResponseEntity.ok(userService.me(user))

    @Operation(summary = "본인 정보 수정", description = "로그인한 사용자의 정보를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공"),
            ApiResponse(responseCode = "400", description = "BAD_NAME - 이름 형식 오류 | BAD_PASSWORD - 비밀번호 형식 오류 | PROFILE_IMAGE_NOT_FOUND - 존재하지 않는 이미지"),
            ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - 로그인이 필요합니다"),
            ApiResponse(responseCode = "403", description = "EMAIL_CHANGE_FORBIDDEN - 이메일은 변경할 수 없습니다"),
        ],
    )
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

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자의 계정을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
            ApiResponse(responseCode = "401", description = "AUTHENTICATION_REQUIRED - 로그인이 필요합니다"),
            ApiResponse(responseCode = "404", description = "NO_SUCH_USER - 존재하지 않는 회원입니다"),
        ],
    )
    @DeleteMapping("/me")
    fun deleteMe(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ResponseEntity<Void> {
        userService.deleteMe(user = user)
        return ResponseEntity.noContent().build()
    }
}
