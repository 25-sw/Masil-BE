package sw2025.masil.global.security.jwt.exception

import sw2025.masil.global.error.exception.ErrorCode
import sw2025.masil.global.error.exception.MasilException

object InvaildJwtException : MasilException(
    ErrorCode.INVALID_TOKEN
)
