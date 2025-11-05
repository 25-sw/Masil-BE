package sw2025.masil.global.error.exception

abstract class MasilException(
    val errorCode: ErrorCode
) : RuntimeException()
