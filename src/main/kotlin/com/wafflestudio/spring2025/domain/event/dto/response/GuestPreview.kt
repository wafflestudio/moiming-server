package com.wafflestudio.spring2025.domain.event.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 이벤트 참여자 미리보기 정보
 * (회원 참여자 기준)
 */
@Schema(description = "참여자 미리보기 정보")
data class GuestPreview(

    @Schema(description = "참여자 ID", example = "1")
    val id: Long,

    @Schema(description = "참여자 이름", example = "홍길동")
    val name: String,

    @Schema(
        description = "프로필 이미지 URL",
        example = "https://example.com/profile.png",
        nullable = true
    )
    val profileImage: String?,
)
