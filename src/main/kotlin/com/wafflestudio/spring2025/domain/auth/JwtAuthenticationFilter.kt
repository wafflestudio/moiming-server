package com.wafflestudio.spring2025.domain.auth

import com.wafflestudio.spring2025.domain.auth.service.JwtBlacklistService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtBlacklistService: JwtBlacklistService,
) : OncePerRequestFilter() {
    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        TODO("JWT 인증 필터 처리 구현")
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        TODO("Authorization 헤더에서 토큰 추출 구현")
    }

    private fun isPublicPath(path: String): Boolean =
        TODO("인증 제외 경로 매칭 구현")
}
