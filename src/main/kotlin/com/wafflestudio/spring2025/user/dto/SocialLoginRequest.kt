package com.wafflestudio.spring2025.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequest (
    @Schema(description = "소셜로그인 제공자", example = "google", required = true)
    val provider: String,
    @Schema(description = "인가 코드 (authorization code)", required = true)
    val code: String,
)

