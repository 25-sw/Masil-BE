package sw2025.masil.domain.storeowner.exception

import sw2025.masil.global.error.exception.ErrorCode
import sw2025.masil.global.error.exception.MasilException

object StoreOwnerNotFoundException : MasilException(
    ErrorCode.STOREOWNER_NOT_FOUND
)
