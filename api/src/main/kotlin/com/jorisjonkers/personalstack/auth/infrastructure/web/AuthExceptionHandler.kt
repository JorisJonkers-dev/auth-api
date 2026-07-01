package com.jorisjonkers.personalstack.auth.infrastructure.web

import com.jorisjonkers.personalstack.common.web.GlobalExceptionHandler
import com.jorisjonkers.personalstack.common.web.ProblemDetail
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.net.URI

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthExceptionHandler {
    private val sharedHandler = GlobalExceptionHandler()

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetail> = sharedHandler.handleValidation(ex, request)

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(
        ex: HandlerMethodValidationException,
        request: WebRequest,
    ): ResponseEntity<ProblemDetail> = sharedHandler.handleHandlerMethodValidation(ex, request)

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest?,
    ): ResponseEntity<ProblemDetail> = sharedHandler.handleConstraintViolation(ex, request)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(): ResponseEntity<ProblemDetail> {
        val body =
            ProblemDetail(
                type = URI.create("https://jorisjonkers.dev/errors/forbidden"),
                title = "Forbidden",
                status = HttpStatus.FORBIDDEN.value(),
                detail = "You do not have permission to perform this action",
            )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body)
    }
}
