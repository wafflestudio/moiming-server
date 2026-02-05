package com.wafflestudio.spring2025.common.email.service

import com.wafflestudio.spring2025.common.email.client.EmailClient
import com.wafflestudio.spring2025.config.EmailConfig
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        val verificationUrl = "${emailConfig.serviceDomain}/auth/verify?verificationCode=$verificationCode"
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

    data class RegistrationStatusEmailData(
        val toEmail: String,
        val status: RegistrationStatus,
        val name: String,
        val eventTitle: String,
        val startsAt: Instant?,
        val endsAt: Instant?,
        val location: String?,
        val confirmedCount: Int?,
        val capacity: Int?,
        val registrationStartsAt: Instant?,
        val registrationEndsAt: Instant?,
        val description: String?,
        val publicId: String?,
        val registrationPublicId: String?,
        val waitingNum: Int? = null,
    )

    fun sendRegistrationStatusEmail(data: RegistrationStatusEmailData) {
        when (data.status) {
            RegistrationStatus.CONFIRMED -> {
                val htmlContent =
                    loadTemplate("registration-confirmed.html")
                        .replace("{name}", data.name)
                        .replace("{eventTitle}", data.eventTitle)
                        .replace("{startsAt}", formatInstant(data.startsAt))
                        .replace("{endsAt}", formatInstant(data.endsAt))
                        .replace("{location}", data.location ?: "-")
                        .replace("{confirmedCount}", data.confirmedCount?.toString() ?: "-")
                        .replace("{capacity}", data.capacity?.toString() ?: "-")
                        .replace("{registrationStartsAt}", formatInstant(data.registrationStartsAt))
                        .replace("{registrationEndsAt}", formatInstant(data.registrationEndsAt))
                        .replace("{description}", data.description ?: "-")
                        .replace("{publicId}", data.publicId ?: "-")
                        .replace("{registrationPublicId}", data.registrationPublicId ?: "-")

                emailClient.sendEmail(
                    to = data.toEmail,
                    subject = "모이샤 참여 신청 확정",
                    htmlContent = htmlContent,
                    fromEmail = emailConfig.fromEmail,
                    fromName = emailConfig.fromName,
                )

                logger.info("신청 확정 정보가 ${data.toEmail} 로 전달되었습니다.")
            }

            RegistrationStatus.WAITLISTED -> {
                val htmlContent =
                    loadTemplate("registration-waitlisted.html")
                        .replace("{name}", data.name)
                        .replace("{waitingNum}", data.waitingNum?.toString() ?: "-")
                        .replace("{eventTitle}", data.eventTitle)
                        .replace("{startsAt}", formatInstant(data.startsAt))
                        .replace("{endsAt}", formatInstant(data.endsAt))
                        .replace("{location}", data.location ?: "-")
                        .replace("{confirmedCount}", data.confirmedCount?.toString() ?: "-")
                        .replace("{capacity}", data.capacity?.toString() ?: "-")
                        .replace("{registrationStartsAt}", formatInstant(data.registrationStartsAt))
                        .replace("{registrationEndsAt}", formatInstant(data.registrationEndsAt))
                        .replace("{description}", data.description ?: "-")
                        .replace("{publicId}", data.publicId ?: "-")
                        .replace("{registrationPublicId}", data.registrationPublicId ?: "-")

                emailClient.sendEmail(
                    to = data.toEmail,
                    subject = "모이샤 참여 신청 대기",
                    htmlContent = htmlContent,
                    fromEmail = emailConfig.fromEmail,
                    fromName = emailConfig.fromName,
                )

                logger.info("신청 대기 정보가 ${data.toEmail} 로 전달되었습니다.")
            }
            RegistrationStatus.CANCELED -> {
                val htmlContent =
                    loadTemplate("registration-canceled.html")
                        .replace("{name}", data.name)
                        .replace("{eventTitle}", data.eventTitle)
                        .replace("{startsAt}", formatInstant(data.startsAt))
                        .replace("{endsAt}", formatInstant(data.endsAt))
                        .replace("{location}", data.location ?: "-")
                        .replace("{confirmedCount}", data.confirmedCount?.toString() ?: "-")
                        .replace("{capacity}", data.capacity?.toString() ?: "-")
                        .replace("{registrationStartsAt}", formatInstant(data.registrationStartsAt))
                        .replace("{registrationEndsAt}", formatInstant(data.registrationEndsAt))
                        .replace("{description}", data.description ?: "-")

                emailClient.sendEmail(
                    to = data.toEmail,
                    subject = "모이샤 참여 신청 취소",
                    htmlContent = htmlContent,
                    fromEmail = emailConfig.fromEmail,
                    fromName = emailConfig.fromName,
                )

                logger.info("신청 취소 정보가 ${data.toEmail} 로 전달되었습니다.")
            }
            RegistrationStatus.BANNED -> {
                val htmlContent =
                    loadTemplate("registration-banned.html")
                        .replace("{name}", data.name)
                        .replace("{eventTitle}", data.eventTitle)
                        .replace("{startsAt}", formatInstant(data.startsAt))
                        .replace("{endsAt}", formatInstant(data.endsAt))
                        .replace("{location}", data.location ?: "-")
                        .replace("{confirmedCount}", data.confirmedCount?.toString() ?: "-")
                        .replace("{capacity}", data.capacity?.toString() ?: "-")
                        .replace("{registrationStartsAt}", formatInstant(data.registrationStartsAt))
                        .replace("{registrationEndsAt}", formatInstant(data.registrationEndsAt))
                        .replace("{description}", data.description ?: "-")

                emailClient.sendEmail(
                    to = data.toEmail,
                    subject = "모이샤 참여 신청 강제 취소",
                    htmlContent = htmlContent,
                    fromEmail = emailConfig.fromEmail,
                    fromName = emailConfig.fromName,
                )

                logger.info("신청 강제 취소 정보가 ${data.toEmail} 로 전달되었습니다.")
            }
            else -> {
                logger.info("신청 상태가 ${data.status} 이라 메일을 전송하지 않았습니다: ${data.toEmail}")
            }
        }
    }

    fun sendWaitlistPromotionEmail(
        toEmail: String,
        eventTitle: String,
        name: String,
        waitingNum: Int?,
        startsAt: Instant?,
        endsAt: Instant?,
        location: String?,
        confirmedCount: Int?,
        capacity: Int?,
        registrationStartsAt: Instant?,
        registrationEndsAt: Instant?,
        description: String?,
        eventPublicId: String,
        registrationPublicId: String,
    ) {
        val htmlContent =
            loadTemplate("registration-waitlist-promoted.html")
                .replace("{name}", name)
                .replace("{waitingNum}", waitingNum?.toString() ?: "-")
                .replace("{eventTitle}", eventTitle)
                .replace("{startsAt}", formatInstant(startsAt))
                .replace("{endsAt}", formatInstant(endsAt))
                .replace("{location}", location ?: "-")
                .replace("{confirmedCount}", confirmedCount?.toString() ?: "-")
                .replace("{capacity}", capacity?.toString() ?: "-")
                .replace("{registrationStartsAt}", formatInstant(registrationStartsAt))
                .replace("{registrationEndsAt}", formatInstant(registrationEndsAt))
                .replace("{description}", description ?: "-")
                .replace("{publicId}", eventPublicId)
                .replace("{registrationPublicId}", registrationPublicId)

        emailClient.sendEmail(
            to = toEmail,
            subject = "모이샤 참여 신청 대기 후, 확정",
            htmlContent = htmlContent,
            fromEmail = emailConfig.fromEmail,
            fromName = emailConfig.fromName,
        )

        logger.info("신청 대기 후 확정 정보가 $toEmail 로 전달되었습니다.")
    }

    private fun formatInstant(instant: Instant?): String =
        instant?.atZone(ZoneId.of("Asia/Seoul"))?.format(KOREAN_DATETIME_FORMATTER) ?: "-"

    companion object {
        private val KOREAN_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    }
}
