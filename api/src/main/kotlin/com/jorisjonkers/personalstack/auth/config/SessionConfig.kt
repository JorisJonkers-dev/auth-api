package com.jorisjonkers.personalstack.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.data.redis.RedisSessionRepository
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession
import org.springframework.session.web.http.CookieHttpSessionIdResolver
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer
import org.springframework.session.web.http.HttpSessionIdResolver
import java.time.Duration

@Configuration
@EnableRedisHttpSession(redisNamespace = "auth-api")
class SessionConfig(
    @param:Value("\${session.cookie.domain:}")
    private val cookieDomain: String,
    @param:Value("\${session.cookie.secure:true}")
    private val secureCookie: Boolean,
    @param:Value("\${session.cookie.same-site:None}")
    private val sameSite: String,
    @param:Value("\${session.timeout:30d}")
    private val sessionTimeout: Duration,
) {
    @Bean
    fun cookieSerializer(): CookieSerializer =
        DefaultCookieSerializer().apply {
            setCookieName("SESSION")
            setCookiePath("/")
            setSameSite(sameSite)
            setUseHttpOnlyCookie(true)
            setCookieMaxAge(sessionTimeout.seconds.toInt())
            if (cookieDomain.isNotBlank()) {
                setDomainName(cookieDomain)
            }
            setUseSecureCookie(secureCookie)
        }

    @Bean
    fun httpSessionIdResolver(): HttpSessionIdResolver =
        CookieHttpSessionIdResolver().apply {
            setCookieSerializer(cookieSerializer())
        }

    /**
     * The generic type has to name the repository `@EnableRedisHttpSession`
     * actually builds, which is `RedisSessionRepository`. A customizer typed for
     * any other repository is never invoked -- no bean fails and nothing is
     * logged, so the annotation's own 30-minute default silently stands. Switching
     * to `@EnableRedisIndexedHttpSession` means changing this type with it.
     */
    @Bean
    fun redisSessionRepositoryCustomizer(): SessionRepositoryCustomizer<RedisSessionRepository> =
        SessionRepositoryCustomizer { sessionRepository ->
            sessionRepository.setDefaultMaxInactiveInterval(sessionTimeout)
        }

    companion object {
        /**
         * Spring Session builds the repository itself, so a BeanPostProcessor is
         * the only seam available to wrap it. Static, so wrapping the repository
         * does not force this configuration class to initialise early.
         */
        @Bean
        @JvmStatic
        fun sessionRepositoryTolerance(): BeanPostProcessor =
            object : BeanPostProcessor {
                override fun postProcessAfterInitialization(
                    bean: Any,
                    beanName: String,
                ): Any =
                    when (bean) {
                        is UnreadableSessionTolerantRepository<*> -> bean
                        is SessionRepository<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            UnreadableSessionTolerantRepository(bean as SessionRepository<Session>)
                        }

                        else -> bean
                    }
            }
    }
}
