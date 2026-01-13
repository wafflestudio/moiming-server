package com.wafflestudio.spring2025.domain.auth.service

import com.wafflestudio.spring2025.domain.auth.JwtTokenProvider
import com.wafflestudio.spring2025.domain.user.dto.core.UserDto
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisTemplate: StringRedisTemplate,
    private val jwtBlacklistService: JwtBlacklistService,
) {
    fun register(
        email: String,
        name: String,
        profileImage: String?,
    ): UserDto {
        TODO("회원가입 도메인 로직 구현")
    }

    fun socialRegister(
        // TODO
    ): UserDto {
        TODO("소셜 회원가입 도메인 로직 구현")
    }

    fun login(email: String): String {
        TODO("로그인 도메인 로직 구현")
    }

    fun logout(
        user: User,
        token: String,
    ) {
        TODO("로그아웃 도메인 로직 구현")
    }
}
