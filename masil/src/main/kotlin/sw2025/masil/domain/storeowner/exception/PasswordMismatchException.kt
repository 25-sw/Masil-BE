package sw2025.masil.domain.storeowner.exception

import sw2025.masil.global.error.exception.ErrorCode
import sw2025.masil.global.error.exception.MasilException

object PasswordMismatchException : MasilException(
    ErrorCode.PASSWORD_MISMATCH
)
