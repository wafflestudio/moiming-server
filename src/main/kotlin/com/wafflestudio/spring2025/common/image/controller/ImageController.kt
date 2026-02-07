package com.wafflestudio.spring2025.common.image.controller

import com.wafflestudio.spring2025.common.image.dto.ImageUploadResponse
import com.wafflestudio.spring2025.common.image.service.ImageService
import com.wafflestudio.spring2025.domain.auth.AuthRequired
import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@AuthRequired
@RestController
@RequestMapping("/api/images")
class ImageController(
    private val imageService: ImageService,
) {
    @Operation(summary = "이미지 업로드", description = "서버 저장소에 이미지를 업로드합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 누락/형식 오류 등)"),
            ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun uploadImage(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestPart("image") image: MultipartFile,
        @Parameter(description = "이미지를 저장할 상위 경로", required = false)
        @RequestParam(name = "prefix", required = false)
        prefix: String?,
    ): ResponseEntity<ImageUploadResponse> {
        val userId = requireNotNull(user.id) { "로그인 사용자 ID가 없습니다." }
        val response = imageService.uploadImage(ownerId = userId, image = image, prefix = prefix)
        return ResponseEntity.ok(response)
    }
}
