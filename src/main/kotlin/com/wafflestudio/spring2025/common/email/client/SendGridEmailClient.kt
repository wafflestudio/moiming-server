package com.wafflestudio.spring2025.common.email.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class SendGridEmailClient(
    private val webClient: WebClient,
    @Value("\${sendgrid.api-key}") private val apiKey: String,
) : EmailClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send"
    }

    override fun sendEmail(
        to: String,
        subject: String,
        htmlContent: String,
        fromEmail: String,
        fromName: String,
    ) {
        val requestBody = buildSendGridRequest(to, subject, htmlContent, fromEmail, fromName)

        try {
            webClient
                .post()
                .uri(SENDGRID_API_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({ it.isError }) { response ->
                    response.bodyToMono<String>().map { body ->
                        logger.error("SendGrid API error: status=${response.statusCode()}, body=$body")
                        RuntimeException("Failed to send email via SendGrid: ${response.statusCode()}")
                    }
                }.bodyToMono<String>()
                .block()

            logger.info("Email sent successfully to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send email to: $to", e)
            throw RuntimeException("Email sending failed", e)
        }
    }

    private fun buildSendGridRequest(
        to: String,
        subject: String,
        htmlContent: String,
        fromEmail: String,
        fromName: String,
    ): Map<String, Any> =
        mapOf(
            "personalizations" to
                listOf(
                    mapOf(
                        "to" to
                            listOf(
                                mapOf("email" to to),
                            ),
                    ),
                ),
            "from" to
                mapOf(
                    "email" to fromEmail,
                    "name" to fromName,
                ),
            "subject" to subject,
            "content" to
                listOf(
                    mapOf(
                        "type" to "text/html",
                        "value" to htmlContent,
                    ),
                ),
        )
}
