package com.wafflestudio.spring2025.common.email.client

/**
 * Interface for email sending clients
 */
interface EmailClient {
    /**
     * Sends an email with HTML content
     * @param to recipient email address
     * @param subject email subject
     * @param htmlContent HTML content of the email
     * @param fromEmail sender email address
     * @param fromName sender name
     */
    fun sendEmail(
        to: String,
        subject: String,
        htmlContent: String,
        fromEmail: String,
        fromName: String,
    )
}
