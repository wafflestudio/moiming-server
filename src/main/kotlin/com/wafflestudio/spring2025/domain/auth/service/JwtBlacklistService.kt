package com.wafflestudio.spring2025.domain.auth.service

import com.wafflestudio.spring2025.domain.auth.JwtTokenProvider
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class JwtBlacklistService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun addToBlacklist(token: String) {
        val jti = jwtTokenProvider.getJti(token) ?: return

        val expiration = jwtTokenProvider.getExpiration(token).time
        val now = System.currentTimeMillis()
        val ttl = expiration - now

        if (ttl <= 0) {
            return
        }

        redisTemplate
            .opsForValue()
            .set("$BLACKLIST_PREFIX$jti", "1", ttl, TimeUnit.MILLISECONDS)
    }

    fun isBlacklisted(token: String): Boolean {
        val jti = jwtTokenProvider.getJti(token) ?: return false
        redisTemplate.opsForValue().get("$BLACKLIST_PREFIX$jti") ?: return false
        return true
    }

    companion object {
        private const val BLACKLIST_PREFIX = "jwt:blacklist:"
    }
}
