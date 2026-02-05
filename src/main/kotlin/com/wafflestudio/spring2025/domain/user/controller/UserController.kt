package com.wafflestudio.spring2025.domain.user.controller

import com.wafflestudio.spring2025.domain.auth.AuthRequired
import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.user.dto.GetMeResponse
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

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
    @AuthRequired
    @GetMapping("/me")
    fun me(
        @Parameter(hidden = true) @LoggedInUser user: User?,
    ): ResponseEntity<GetMeResponse> = ResponseEntity.ok(userService.me(user))

    @Operation(summary = "프로필 이미지 업로드", description = "로그인한 사용자의 프로필 이미지를 업로드(교체)합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "프로필 이미지 업로드 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 누락/형식 오류 등)"),
            ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @AuthRequired
    @PutMapping(
        "/me/profile-image",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun uploadProfileImage(
        @Parameter(hidden = true) @LoggedInUser user: User?,
        @RequestPart("image") image: MultipartFile,
    ): ResponseEntity<Void> {
        userService.updateProfileImage(user, image)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "프로필 이미지 삭제", description = "로그인한 사용자의 프로필 이미지를 삭제(기본 이미지로 복귀)합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "프로필 이미지 삭제 성공"),
            ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @AuthRequired
    @DeleteMapping("/me/profile-image")
    fun deleteProfileImage(
        @Parameter(hidden = true) @LoggedInUser user: User?,
    ): ResponseEntity<Void> {
        userService.deleteProfileImage(user)
        return ResponseEntity.noContent().build()
    }
}
