package com.jorisjonkers.personalstack.auth.flow

import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import com.jorisjonkers.personalstack.auth.infrastructure.email.EmailConfirmationEmailListener
import com.jorisjonkers.personalstack.auth.infrastructure.email.PasswordResetEmailListener
import com.jorisjonkers.personalstack.common.email.EmailService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender

/**
 * Asserts that this application can actually send mail.
 *
 * EmailService used to carry @ConditionalOnBean(JavaMailSender::class) while
 * being component-scanned. That condition is evaluated before auto-configuration
 * contributes JavaMailSender, so the bean was never registered -- and both
 * listeners took Optional<EmailService>, found it empty, logged at DEBUG and
 * returned. Confirmation and password-reset mail were never sent and never
 * errored: a registration returning 201 opened no SMTP session at all.
 *
 * Every assertion here is about wiring rather than behaviour, because wiring is
 * what failed. The listeners now take EmailService directly, so a regression
 * fails context startup instead of silently disabling email.
 */
class EmailWiringIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var mailSender: JavaMailSender

    @Autowired
    private lateinit var confirmationListener: EmailConfirmationEmailListener

    @Autowired
    private lateinit var passwordResetListener: PasswordResetEmailListener

    @Test
    fun `the application context can send email`() {
        assertThat(mailSender).isNotNull()
        assertThat(emailService).isNotNull()
    }

    @Test
    fun `both email listeners are wired to a real EmailService`() {
        // Injecting them at all is the assertion: each now declares
        // EmailService as a required constructor dependency, so the context
        // could not have started without one.
        assertThat(confirmationListener).isNotNull()
        assertThat(passwordResetListener).isNotNull()
    }
}
