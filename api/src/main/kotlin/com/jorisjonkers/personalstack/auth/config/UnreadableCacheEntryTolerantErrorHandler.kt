package com.jorisjonkers.personalstack.auth.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.data.redis.serializer.SerializationException

/**
 * Makes a cached user this build cannot read behave like a cache miss.
 *
 * Cached users are JSON of the domain [com.jorisjonkers.personalstack.auth.domain.model.User],
 * so retiring a ServicePermission strands entries that still name it until their TTL runs out,
 * and Spring would otherwise rethrow the `SerializationException` on every read of that user.
 * The entry is evicted and the caller falls through to Postgres. Every other cache failure,
 * including a Valkey outage and a failed eviction, still propagates.
 */
class UnreadableCacheEntryTolerantErrorHandler : CacheErrorHandler {
    override fun handleCacheGetError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        if (exception !is SerializationException) throw exception
        log.warn("Discarding cache entry {} in {}: unreadable by this build", key, cache.name, exception)
        cache.evict(key)
    }

    override fun handleCachePutError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
        value: Any?,
    ): Unit = throw exception

    override fun handleCacheEvictError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ): Unit = throw exception

    override fun handleCacheClearError(
        exception: RuntimeException,
        cache: Cache,
    ): Unit = throw exception

    private companion object {
        val log: Logger = LoggerFactory.getLogger(UnreadableCacheEntryTolerantErrorHandler::class.java)
    }
}
