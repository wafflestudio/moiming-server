package com.wafflestudio.spring2025.domain.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class JwtBlacklistService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${jwt.expiration-in-ms}")
    private val expirationInMs: Long,
) {
    fun addToBlacklist(token: String) {
        TODO("JWT 블랙리스트 등록 구현")
    }

    fun isBlacklisted(token: String): Boolean {
        TODO("JWT 블랙리스트 조회 구현")
    }

    companion object {
        private const val BLACKLIST_PREFIX = "jwt:blacklist:"
    }
}
