package com.jorisjonkers.personalstack.auth.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.yaml.snakeyaml.Yaml
import java.time.Duration

/**
 * The Redis timeouts must leave room for a reconnect handshake.
 *
 * Spring Boot builds Lettuce with TimeoutOptions.enabled(), so
 * spring.data.redis.timeout bounds more than steady-state commands -- it also
 * caps the handshake performed when a dropped connection is re-established.
 *
 * This was set to 500ms, which held right up until the endpoint dropped in
 * production: the next request reconnected, the handshake exceeded the budget,
 * and the caller got
 *
 *   RedisConnectionFailureException: Unable to connect to Redis
 *
 * as an HTTP 500. Redis backs @EnableRedisHttpSession, so the blast radius is
 * every authenticated request, not just the cached reads.
 *
 * Valkey serves this cluster in ~2ms with an 84ms worst case, so the floor
 * below is not about the server being slow. It is headroom for the client: a GC
 * pause or CFS throttling landing on top of a reconnect. Lowering these back
 * under a second re-opens exactly that failure.
 */
class RedisTimeoutConfigTest {
    private val floor = Duration.ofSeconds(1)

    private fun redisConfig(): Map<*, *> {
        val yaml = ClassPathResource("application.yml").inputStream.use { Yaml().load<Map<*, *>>(it) }
        val spring = yaml["spring"] as Map<*, *>
        val data = spring["data"] as Map<*, *>
        return data["redis"] as Map<*, *>
    }

    @Test
    fun `command timeout leaves room for a reconnect handshake`() {
        val timeout = redisConfig()["timeout"]

        assertThat(timeout)
            .describedAs("spring.data.redis.timeout must be set explicitly")
            .isNotNull()
        assertThat(parse(timeout.toString()))
            .describedAs("spring.data.redis.timeout also bounds the Lettuce reconnect handshake")
            .isGreaterThanOrEqualTo(floor)
    }

    @Test
    fun `connect timeout is set explicitly`() {
        // Left unset, the handshake budget silently governs how long a dead
        // node takes to fail, which is not a thing to discover during an
        // incident.
        val connectTimeout = redisConfig()["connect-timeout"]

        assertThat(connectTimeout)
            .describedAs("spring.data.redis.connect-timeout must be set explicitly")
            .isNotNull()
        assertThat(parse(connectTimeout.toString())).isGreaterThanOrEqualTo(floor)
    }

    private fun parse(value: String): Duration =
        when {
            value.endsWith("ms") -> Duration.ofMillis(value.removeSuffix("ms").trim().toLong())
            value.endsWith("s") -> Duration.ofSeconds(value.removeSuffix("s").trim().toLong())
            else -> Duration.ofMillis(value.trim().toLong())
        }
}
