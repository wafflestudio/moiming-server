package com.wafflestudio.spring2025.domain.user.dto

data class PatchMeRequest(
    val name: String?,
    val email: String?,
    val password: String?,
    val profileImage: String?,
)
