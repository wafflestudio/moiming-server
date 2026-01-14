package com.wafflestudio.spring2025.domain.user.identity.service

import com.wafflestudio.spring2025.domain.user.identity.dto.core.UserIdentityDto
import com.wafflestudio.spring2025.domain.user.identity.repository.UserIdentityRepository
import org.springframework.stereotype.Service

@Service
class UserIdentityService(
    private val userIdentityRepository: UserIdentityRepository,
) {
    fun linkIdentity(
        userId: Long,
        provider: String,
        providerUserId: String,
    ): UserIdentityDto {
        TODO("소셜 계정 연동 구현")
    }

    fun getIdentitiesByUserId(userId: Long): List<UserIdentityDto> {
        TODO("사용자의 소셜 계정 목록 조회 구현")
    }

    fun unlinkIdentity(
        userId: Long,
        identityId: Long,
    ) {
        TODO("소셜 계정 연동 해제 구현")
    }
}
