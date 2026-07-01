package com.jorisjonkers.personalstack.auth.infrastructure.web

import com.jorisjonkers.personalstack.common.web.GlobalExceptionHandler
import com.jorisjonkers.personalstack.common.web.ProblemDetail
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import tools.jackson.databind.ObjectMapper

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ValidationExceptionResolver(
    private val objectMapper: ObjectMapper,
) : HandlerExceptionResolver {
    private val sharedHandler = GlobalExceptionHandler()

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception,
    ): ModelAndView? {
        val webRequest = ServletWebRequest(request)
        val entity =
            when (ex) {
                is MethodArgumentNotValidException -> sharedHandler.handleValidation(ex, webRequest)
                is HandlerMethodValidationException -> sharedHandler.handleHandlerMethodValidation(ex, webRequest)
                is ConstraintViolationException -> sharedHandler.handleConstraintViolation(ex, webRequest)
                else -> return null
            }
        write(response, entity)
        return ModelAndView()
    }

    private fun write(
        response: HttpServletResponse,
        entity: ResponseEntity<ProblemDetail>,
    ) {
        response.status = entity.statusCode.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(response.outputStream, entity.body)
    }
}
