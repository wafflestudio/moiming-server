package com.wafflestudio.spring2025.common.email.client

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Primary
@Component
class JavaMailEmailClient(
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
        logger.info("메일링 임시 중단으로 발송 생략: $to")
        return
        try {
            val message: MimeMessage = javaMailSender.createMimeMessage()

            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setTo(to)
            helper.setSubject(subject)

            helper.setText(htmlContent, true)

            helper.setFrom("$fromName <$fromEmail>")

            javaMailSender.send(message)

            logger.info("Gmail을 통해 메일 발송 성공: $to")
        } catch (e: Exception) {
            logger.error("Gmail 메일 발송 실패: $to", e)
            throw RuntimeException("Email sending failed via Gmail", e)
        }
    }
}
