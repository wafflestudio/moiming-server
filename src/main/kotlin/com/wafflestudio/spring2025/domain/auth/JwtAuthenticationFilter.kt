package com.wafflestudio.spring2025.domain.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    // private val jwtBlacklistService: JwtBlacklistService,
) : OncePerRequestFilter() {
    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (isPublicPath(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveToken(request)

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing token")
            return
        }

//        val jti = jwtTokenProvider.getJti(token)
//        if (jti != null && jwtBlacklistService.isBlacklisted(jti)) {
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted")
//            return
//        }

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

    private fun isPublicPath(path: String): Boolean =
        pathMatcher.match("/api/v1/auth/**", path) ||
            pathMatcher.match("/swagger-ui/**", path) ||
            pathMatcher.match("/v3/api-docs/**", path) ||
            pathMatcher.match("/actuator/health", path)
}
