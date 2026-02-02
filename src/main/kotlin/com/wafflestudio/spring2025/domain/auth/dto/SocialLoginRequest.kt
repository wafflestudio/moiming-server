package com.wafflestudio.spring2025.domain.auth.dto

import com.wafflestudio.spring2025.domain.auth.model.SocialProvider

data class SocialLoginRequest(
    val code: String,
    val provider: SocialProvider,
)
