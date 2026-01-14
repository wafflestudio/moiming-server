package com.wafflestudio.spring2025.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "email")
class EmailConfig {
    var fromEmail: String = System.getenv("EMAIL_FROM_ADDRESS") ?: "noreply@example.com"
    var fromName: String = "모이샤"
    var serviceDomain: String = System.getenv("EMAIL_SERVICE_DOMAIN") ?: "http://localhost:8080"
}
