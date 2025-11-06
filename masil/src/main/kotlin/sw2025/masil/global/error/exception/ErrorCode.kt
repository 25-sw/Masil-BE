package sw2025.masil.global.error.exception

enum class ErrorCode(
    val status: Int,
    val message: String
) {
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),
    PASSWORD_MISMATCH(401, "비밀번호가 일치하지 않습니다."),

    INVALID_TOKEN(403, "잘못된 토큰입니다."),

    STOREOWNER_NOT_FOUND(404, "존재하지 않는 유저입니다."),

    DUPLICATION_ACCOUNT_ID(409, "이미 존재하는 아이디입니다."),

    S3_UPLOAD_FAILED(500, "이미지 업로드에 실패했습니다.")
}
