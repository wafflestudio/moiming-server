package com.wafflestudio.spring2025.common.email.service

import com.wafflestudio.spring2025.common.email.client.EmailClient
import com.wafflestudio.spring2025.config.EmailConfig
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailClient: EmailClient,
    private val emailConfig: EmailConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Sends a verification email to the specified email address
     * @param toEmail recipient email address
     * @param verificationCode verification code to include in the URL
     */
    fun sendVerificationEmail(
        toEmail: String,
        verificationCode: String,
    ) {
        val verificationUrl = "${emailConfig.serviceDomain}/verify?code=$verificationCode"
        val htmlContent =
            loadTemplate("email-verification.html")
                .replace("{verificationUrl}", verificationUrl)

        try {
            emailClient.sendEmail(
                to = toEmail,
                subject = "모이샤 이메일 인증",
                htmlContent = htmlContent,
                fromEmail = emailConfig.fromEmail,
                fromName = emailConfig.fromName,
            )
        } catch (e: Exception) {
            // throw EmailServiceUnavailableException()
            throw e
        }

        logger.info("Verification email sent to: $toEmail")
    }

    /**
     * Loads an email template from the template directory
     * @param templateName name of the template file
     * @return template content as string
     */
    private fun loadTemplate(templateName: String): String =
        try {
            val resource = ClassPathResource("com/wafflestudio/spring2025/common/email/template/$templateName")
            resource.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            logger.error("Failed to load email template: $templateName", e)
            throw IllegalStateException("Email template not found: $templateName", e)
        }
}
