package com.jorisjonkers.personalstack.auth.config

import org.slf4j.LoggerFactory
import org.springframework.data.redis.serializer.SerializationException
import org.springframework.session.Session
import org.springframework.session.SessionRepository

/**
 * Makes a session this build cannot read behave like no session at all.
 *
 * The stored `SecurityContext` is a JDK-serialized graph of application
 * classes, so a class-shape change can strand rows in Valkey that the running
 * build cannot deserialize. Spring Session lets that `SerializationException`
 * out of `findById`, and it escapes from inside the security filter chain
 * while the deferred context loads — before any controller, so the caller gets
 * a bodyless 401 and the request log stays empty. The browser then re-presents
 * the same `SESSION` cookie on every retry, sign-in included, so the account
 * cannot recover on its own.
 *
 * Dropping the row and answering as an anonymous request is what lets the next
 * sign-in succeed. A store outage is deliberately not caught: serving every
 * request anonymously because Valkey is unreachable would sign out the whole
 * estate silently.
 */
class UnreadableSessionTolerantRepository<S : Session>(
    private val delegate: SessionRepository<S>,
) : SessionRepository<S> by delegate {
    override fun findById(id: String): S? =
        try {
            delegate.findById(id)
        } catch (ex: SerializationException) {
            log.warn("Discarding session {}: stored payload is unreadable by this build", id, ex)
            discard(id)
            null
        }

    private fun discard(id: String) {
        try {
            delegate.deleteById(id)
        } catch (ex: SerializationException) {
            // Some stores read the row before removing it, so the delete can fail
            // the same way the read did. The session is still unusable; let the
            // caller continue anonymously and let the TTL collect the row.
            log.warn("Could not delete unreadable session {}; leaving it to expire", id, ex)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(UnreadableSessionTolerantRepository::class.java)
    }
}
