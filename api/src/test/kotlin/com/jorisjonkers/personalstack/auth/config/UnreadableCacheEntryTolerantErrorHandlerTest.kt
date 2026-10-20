package com.jorisjonkers.personalstack.auth.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.serializer.SerializationException

class UnreadableCacheEntryTolerantErrorHandlerTest {
    private val cache = mockk<Cache>(relaxed = true) { every { name } returns "users.byId" }
    private val handler = UnreadableCacheEntryTolerantErrorHandler()

    @Test
    fun `treats an unreadable entry as a miss and evicts it`() {
        assertThatCode {
            handler.handleCacheGetError(SerializationException("No enum constant"), cache, "key")
        }.doesNotThrowAnyException()

        verify(exactly = 1) { cache.evict("key") }
    }

    @Test
    fun `still fails a read when the store is unreachable`() {
        val outage = RedisConnectionFailureException("Valkey down")

        assertThatThrownBy { handler.handleCacheGetError(outage, cache, "key") }.isSameAs(outage)
        verify(exactly = 0) { cache.evict(any()) }
    }

    @Test
    fun `still fails an eviction, so a revoked grant is never served from cache`() {
        val failure = RedisConnectionFailureException("Valkey down")

        assertThatThrownBy { handler.handleCacheEvictError(failure, cache, "key") }.isSameAs(failure)
    }

    @Test
    fun `still fails a write`() {
        val failure = SerializationException("Cannot serialize")

        assertThatThrownBy { handler.handleCachePutError(failure, cache, "key", "value") }.isSameAs(failure)
    }

    @Test
    fun `still fails a clear`() {
        val failure = RedisConnectionFailureException("Valkey down")

        assertThatThrownBy { handler.handleCacheClearError(failure, cache) }.isSameAs(failure)
    }
}
