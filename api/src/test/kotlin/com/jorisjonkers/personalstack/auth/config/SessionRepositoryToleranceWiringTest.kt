package com.jorisjonkers.personalstack.auth.config

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.session.MapSession
import org.springframework.session.SessionRepository

/**
 * The tolerant wrapper is worthless unless Spring Session actually uses it.
 *
 * `@EnableRedisHttpSession` builds the repository itself, so the only seam is a
 * `BeanPostProcessor`. Wrapping the wrong bean, or none, leaves the lockout in
 * place while every unit test around the wrapper still passes.
 */
class SessionRepositoryToleranceWiringTest {
    private val postProcessor = SessionConfig.sessionRepositoryTolerance()

    @Test
    fun `wraps the session repository Spring Session will inject`() {
        val delegate = mockk<SessionRepository<MapSession>>(relaxed = true)

        val processed = postProcessor.postProcessAfterInitialization(delegate, "sessionRepository")

        assertThat(processed).isInstanceOf(UnreadableSessionTolerantRepository::class.java)
    }

    @Test
    fun `leaves every other bean alone`() {
        val unrelated = "not a session repository"

        val processed = postProcessor.postProcessAfterInitialization(unrelated, "someBean")

        assertThat(processed).isSameAs(unrelated)
    }

    @Test
    fun `does not wrap twice`() {
        val delegate = mockk<SessionRepository<MapSession>>(relaxed = true)
        val once = postProcessor.postProcessAfterInitialization(delegate, "sessionRepository")

        val twice = postProcessor.postProcessAfterInitialization(once!!, "sessionRepository")

        assertThat(twice).isSameAs(once)
    }
}
