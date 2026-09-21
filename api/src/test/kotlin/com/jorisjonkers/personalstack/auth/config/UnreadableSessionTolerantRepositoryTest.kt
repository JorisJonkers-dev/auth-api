package com.jorisjonkers.personalstack.auth.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.session.MapSession
import org.springframework.session.SessionRepository

/**
 * An unreadable session must look like no session at all.
 *
 * The stored `SecurityContext` is a JDK-serialized graph of application
 * classes, so a class-shape change can leave a session in Valkey that this
 * build cannot read. Spring Session lets the `SerializationException` out of
 * `findById`, which surfaces as a bodyless 401 from inside the security filter
 * chain — and the browser keeps presenting the same `SESSION` cookie, so
 * signing in again hits the same row. The account is locked out until someone
 * clears cookies or the key expires.
 *
 * Pinning `serialVersionUID` stops that class of break from recurring, but it
 * cannot reach the sessions already written with the wrong id. Dropping the
 * unreadable row and continuing anonymously does: the next request has no
 * session, so sign-in works.
 *
 * A Valkey outage is deliberately not swallowed. Serving an anonymous request
 * because the store is unreachable would silently sign out every user, so that
 * one still fails loudly.
 */
class UnreadableSessionTolerantRepositoryTest {
    private val delegate = mockk<SessionRepository<MapSession>>(relaxed = true)
    private val repository = UnreadableSessionTolerantRepository(delegate)

    @Test
    fun `passes a readable session straight through`() {
        val session = MapSession("readable")
        every { delegate.findById("readable") } returns session

        assertThat(repository.findById("readable")).isSameAs(session)
        verify(exactly = 0) { delegate.deleteById(any()) }
    }

    @Test
    fun `treats an undeserializable session as absent and deletes it`() {
        every { delegate.findById("poisoned") } throws SerializationException("Cannot deserialize")

        assertThat(repository.findById("poisoned")).isNull()
        verify(exactly = 1) { delegate.deleteById("poisoned") }
    }

    @Test
    fun `still deletes the session when it cannot report a missing one`() {
        every { delegate.findById("poisoned") } throws SerializationException("Cannot deserialize")
        every { delegate.deleteById("poisoned") } throws SerializationException("Cannot deserialize")

        assertThat(repository.findById("poisoned")).isNull()
    }

    @Test
    fun `lets a store outage fail rather than signing everyone out`() {
        every { delegate.findById("any") } throws RedisConnectionFailureException("Unable to connect to Redis")

        assertThatThrownBy { repository.findById("any") }
            .isInstanceOf(RedisConnectionFailureException::class.java)
        verify(exactly = 0) { delegate.deleteById(any()) }
    }

    @Test
    fun `delegates the rest of the contract unchanged`() {
        val created = MapSession("created")
        every { delegate.createSession() } returns created

        assertThat(repository.createSession()).isSameAs(created)
        repository.save(created)
        repository.deleteById("gone")

        verify { delegate.save(created) }
        verify { delegate.deleteById("gone") }
    }
}
