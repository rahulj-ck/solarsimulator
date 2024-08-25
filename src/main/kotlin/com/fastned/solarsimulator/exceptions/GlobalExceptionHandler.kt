package com.fastned.solarsimulator.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice
@RestController
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        val errorDetails =
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "status" to HttpStatus.BAD_REQUEST.value(),
                "error" to e.localizedMessage,
            )
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, Any>> {
        val errorDetails =
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error" to "An unexpected error occurred: ${e.localizedMessage}",
            )
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
