package com.jorisjonkers.personalstack.auth.infrastructure.web

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import org.springframework.core.MethodParameter
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.bind.MethodArgumentNotValidException

private val requestBodyValidator by lazy {
    Validation.buildDefaultValidatorFactory().validator
}

private val validationMethodParameter by lazy {
    MethodParameter(Any::class.java.getMethod("toString"), -1)
}

fun validateRequestBody(request: Any) {
    val violations =
        requestBodyValidator
            .validate(request)
            .sortedWith(
                compareBy<ConstraintViolation<Any>> { it.propertyPath.toString() }
                    .thenBy { it.message },
            )
    if (violations.isEmpty()) return

    val bindingResult =
        BeanPropertyBindingResult(
            request,
            request.javaClass.simpleName.replaceFirstChar { it.lowercase() },
        )
    for (violation in violations) {
        val field = violation.propertyPath.toString()
        val code = violation.constraintDescriptor.annotation.annotationClass.java.simpleName
        if (field.isBlank()) {
            bindingResult.reject(code, violation.message)
        } else {
            bindingResult.rejectValue(field, code, violation.message)
        }
    }
    throw MethodArgumentNotValidException(validationMethodParameter, bindingResult)
}
