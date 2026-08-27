package com.jorisjonkers.personalstack.auth.infrastructure.email

import com.jorisjonkers.personalstack.auth.domain.event.PasswordResetRequestedEvent
import com.jorisjonkers.personalstack.common.email.EmailRequest
import com.jorisjonkers.personalstack.common.email.EmailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PasswordResetEmailListener(
    private val emailService: EmailService,
    @param:Value("\${app.password-reset-url:http://localhost:5174/reset-password}")
    private val passwordResetBaseUrl: String,
) {
    @EventListener
    fun onPasswordResetRequested(event: PasswordResetRequestedEvent) {
        val resetUrl = "$passwordResetBaseUrl?token=${event.resetToken}"
        val (textBody, htmlBody) = AuthEmailTemplates.passwordResetEmail(event.username, resetUrl)
        emailService.send(
            EmailRequest(
                to = event.email,
                subject = "Reset your password — jorisjonkers.dev",
                textBody = textBody,
                htmlBody = htmlBody,
            ),
        )
    }
}
