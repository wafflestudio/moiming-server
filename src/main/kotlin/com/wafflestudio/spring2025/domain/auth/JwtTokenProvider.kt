package com.wafflestudio.spring2025.domain.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.expiration-in-ms}")
    private val expirationInMs: Long,
) {
    private val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun createToken(userId: Long): String {
        val now = Date()
        val validity = Date(now.time + expirationInMs)

        val jti = UUID.randomUUID().toString()

        return Jwts
            .builder()
            .setSubject(userId.toString())
            .setId(jti)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getUserId(token: String): Long =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
            .toLong()

    fun getJti(token: String): String? =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .id

    fun getExpiration(token: String): Date =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .expiration

    fun validateToken(token: String): Boolean {
        try {
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            return true
        } catch (e: Exception) {
        }
        return false
    }
}
