package sw2025.masil.global.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import sw2025.masil.global.error.exception.ErrorResponse
import sw2025.masil.global.error.exception.MasilException

@RestControllerAdvice(annotations = [RestController::class])
class GlobalExceptionHandler {
    @ExceptionHandler(MasilException::class)
    fun handlingMasilException(e: MasilException): ResponseEntity<ErrorResponse> {
        val code = e.errorCode
        return ResponseEntity(
            ErrorResponse(code.status, code.message),
            HttpStatus.valueOf(code.status)
        )
    }
}
