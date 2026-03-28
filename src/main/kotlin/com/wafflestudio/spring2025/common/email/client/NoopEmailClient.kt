package com.wafflestudio.spring2025.common.email.client

import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class NoopEmailClient(
    private val javaMailSender: JavaMailSender,
) : EmailClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendEmail(
        to: String,
        subject: String,
        htmlContent: String,
        fromEmail: String,
        fromName: String,
    ) {
        logger.info("메일링 중단으로 발송 생략: $to")
    }
}
