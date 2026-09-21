package com.jorisjonkers.personalstack.auth.flow

import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import java.time.Duration

/**
 * A session the repository creates must expire on `session.timeout`, not on
 * Spring Session's own 30-minute default.
 *
 * `SessionConfig` declares a `SessionRepositoryCustomizer<RedisIndexedSessionRepository>`
 * to apply it, but `@EnableRedisHttpSession` builds a plain
 * `RedisSessionRepository` — the production stack trace from the sign-in
 * lockout names that class. A customizer whose generic type does not match the
 * repository is simply never invoked: no bean fails, nothing is logged, and
 * the annotation's default stands.
 *
 * `SessionLoginController` sets `maxInactiveInterval` by hand on the session it
 * creates, which is why a signed-in browser was unaffected and this stayed
 * invisible. Every other session — the one an OAuth2 authorize hop creates
 * before sign-in, the one a CSRF token lands in — took 30 minutes.
 *
 * Asserting on a session the repository actually produces keeps this honest.
 * A test that asserted the customizer bean exists would have passed throughout.
 */
class SessionTimeoutIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var sessionRepository: SessionRepository<out Session>

    @Value("\${session.timeout}")
    private lateinit var configuredTimeout: Duration

    @Test
    fun `a new session carries the configured timeout`() {
        val session = sessionRepository.createSession()

        assertThat(session.maxInactiveInterval).isEqualTo(configuredTimeout)
        assertThat(session.maxInactiveInterval)
            .describedAs("Spring Session's own default is 30 minutes")
            .isNotEqualTo(Duration.ofMinutes(30))
    }

    @Test
    fun `the configured timeout is the thirty days production runs with`() {
        assertThat(configuredTimeout).isEqualTo(Duration.ofDays(30))
    }
}
