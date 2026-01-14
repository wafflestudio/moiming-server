package com.wafflestudio.spring2025.domain.auth

import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.expiration-in-ms}")
    private val expirationInMs: Long,
) {
    private val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun createToken(email: String): String {
        TODO("JWT 토큰 생성 구현")
    }

    fun getEmail(token: String): String {
        TODO("JWT에서 이메일 추출 구현")
    }

    fun validateToken(token: String): Boolean {
        TODO("JWT 유효성 검증 구현")
    }
}
