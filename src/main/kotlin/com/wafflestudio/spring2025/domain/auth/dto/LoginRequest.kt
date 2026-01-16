package com.wafflestudio.spring2025.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 요청")
data class LoginRequest(
    @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    val email: String,
    @Schema(description = "사용자 비밀번호", example = "mypassword", required = true)
    val password: String,
)
