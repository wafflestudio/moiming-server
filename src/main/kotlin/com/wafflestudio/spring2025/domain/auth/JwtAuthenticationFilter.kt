package com.wafflestudio.spring2025.domain.auth

import com.wafflestudio.spring2025.domain.auth.service.JwtBlacklistService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtBlacklistService: JwtBlacklistService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // CORS preflight 요청은 인증 없이 통과
        if (request.method == "OPTIONS") {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveToken(request)

        if (token == null || !jwtTokenProvider.validateToken(token) || jwtBlacklistService.isBlacklisted(token)) {
            filterChain.doFilter(request, response) // 비로그인 상태, userId 채우지 않음
            return
        }

        val userId = jwtTokenProvider.getUserId(token)
        request.setAttribute("userId", userId)

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
