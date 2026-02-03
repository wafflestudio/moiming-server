package com.wafflestudio.spring2025.common.email.service

import com.wafflestudio.spring2025.common.email.client.EmailClient
import com.wafflestudio.spring2025.config.EmailConfig
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

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
            resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            logger.error("Failed to load email template: $templateName", e)
            throw IllegalStateException("Email template not found: $templateName", e)
        }

    fun sendRegistrationStatusEmail(
        toEmail: String,
        eventTitle: String,
        status: RegistrationStatus,
        waitingNum: Int? = null,
        // 신청 메일 템플릿이 확정됨에 따라 인자 수정 가능
    ) {
        when (status) {
            RegistrationStatus.CONFIRMED -> {
                val htmlContent =
                    loadTemplate("registration-confirmed.html")
                        .replace("{eventTitle}", eventTitle)

                emailClient.sendEmail(
                    to = toEmail,
                    subject = "모이샤 참여 신청 확정",
                    htmlContent = htmlContent,
                    fromEmail = emailConfig.fromEmail,
                    fromName = emailConfig.fromName,
                )

                logger.info("신청 확정 정보가 $toEmail 로 전달되었습니다.")
            }
            RegistrationStatus.WAITING -> {
                val htmlContent =
                    loadTemplate("registration-waitlisted.html")
                        .replace("{eventTitle}", eventTitle)
                        .replace("{waitingNum}", waitingNum?.toString() ?: "-")

                emailClient.sendEmail(
                    to = toEmail,
                    subject = "모이샤 참여 신청 대기",
                    htmlContent = htmlContent,
                    fromEmail = emailConfig.fromEmail,
                    fromName = emailConfig.fromName,
                )

                logger.info("신청 대기 정보가 $toEmail 로 전달되었습니다.")
            }
            else -> {
                logger.info("신청 상태가 $status 이라 메일을 전송하지 않았습니다: $toEmail")
            }
        }
    }

    // 대기하다가 정원이 생겨 확정될 때, 보내는 템플릿
    fun sendWaitlistPromotionEmail(
        toEmail: String,
        eventTitle: String,
        // 신청 메일 템플릿이 확정됨에 따라 인자 수정 가능
    ) {
        val htmlContent =
            loadTemplate("registration-waitlist-promoted.html")
                .replace("{eventTitle}", eventTitle)

        emailClient.sendEmail(
            to = toEmail,
            subject = "모이샤 참여 신청 대기 후, 확정",
            htmlContent = htmlContent,
            fromEmail = emailConfig.fromEmail,
            fromName = emailConfig.fromName,
        )

        logger.info("신청 대기 후 확정 정보가 $toEmail 로 전달되었습니다.")
    }
}
