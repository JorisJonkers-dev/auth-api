package com.jorisjonkers.personalstack.auth.config

import com.jorisjonkers.personalstack.common.web.GlobalExceptionHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExceptionHandlerConfig {
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler::class)
    fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()
}
