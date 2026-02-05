package com.wafflestudio.spring2025.domain.auth

import com.wafflestudio.spring2025.domain.auth.exception.AuthenticationRequiredException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthenticationInterceptor : HandlerInterceptor {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        logger.info("AuthenticationInterceptor pre-handle")

        val handlerMethod = handler as? HandlerMethod ?: return true
        logger.info(handlerMethod.toString())

        val authRequired =
            handlerMethod.hasMethodAnnotation(AuthRequired::class.java) ||
                handlerMethod.beanType.isAnnotationPresent(AuthRequired::class.java)

        if (!authRequired) return true

        val userId = request.getAttribute("userId")
        if (userId != null) return true

        throw AuthenticationRequiredException()
    }
}
