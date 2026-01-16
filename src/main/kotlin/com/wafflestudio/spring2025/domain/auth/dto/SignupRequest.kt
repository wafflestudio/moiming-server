package com.wafflestudio.spring2025.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원가입 요청")
data class SignupRequest(
    @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    val email: String,
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    val name: String,
    @Schema(description = "비밀번호", example = "password123", required = true)
    val password: String,
    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile.png")
    val profileImage: String? = null,
)
