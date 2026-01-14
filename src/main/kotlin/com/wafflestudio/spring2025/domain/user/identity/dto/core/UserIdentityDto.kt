package com.wafflestudio.spring2025.domain.user.identity.dto.core

import com.wafflestudio.spring2025.domain.user.identity.model.UserIdentity
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "소셜 계정 정보")
data class UserIdentityDto(
    @Schema(description = "소셜 계정 ID")
    val id: Long,
    @Schema(description = "사용자 ID")
    val userId: Long,
    @Schema(description = "소셜 로그인 제공자")
    val provider: String,
    @Schema(description = "제공자 사용자 ID")
    val providerUserId: String,
    @Schema(description = "생성 일시 (epoch milliseconds)")
    val createdAt: Long,
) {
    constructor(identity: UserIdentity) : this(
        id = TODO("UserIdentity -> UserIdentityDto 매핑 구현"),
        userId = TODO("UserIdentity -> UserIdentityDto 매핑 구현"),
        provider = TODO("UserIdentity -> UserIdentityDto 매핑 구현"),
        providerUserId = TODO("UserIdentity -> UserIdentityDto 매핑 구현"),
        createdAt = TODO("UserIdentity -> UserIdentityDto 매핑 구현"),
    )
}
