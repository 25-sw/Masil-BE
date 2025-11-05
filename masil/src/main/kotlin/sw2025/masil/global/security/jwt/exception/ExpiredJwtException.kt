package sw2025.masil.global.security.jwt.exception

import sw2025.masil.global.error.exception.ErrorCode
import sw2025.masil.global.error.exception.MasilException

object ExpiredJwtException : MasilException(
    ErrorCode.EXPIRED_TOKEN
)
