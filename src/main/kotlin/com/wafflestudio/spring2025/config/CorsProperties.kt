package com.wafflestudio.spring2025.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedOrigins: String = "",
) {
    fun getAllowedOriginsList(): List<String> = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
