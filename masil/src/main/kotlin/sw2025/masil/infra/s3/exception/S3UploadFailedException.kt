package sw2025.masil.infra.s3.exception

import sw2025.masil.global.error.exception.ErrorCode
import sw2025.masil.global.error.exception.MasilException

object S3UploadFailedException : MasilException(
    ErrorCode.S3_UPLOAD_FAILED
)