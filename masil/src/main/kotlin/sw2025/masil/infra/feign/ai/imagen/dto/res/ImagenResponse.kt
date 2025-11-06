package sw2025.masil.infra.feign.ai.imagen.dto.res

data class ImagenResponse(
    val predictions: List<Prediction>?,
    val metadata: Metadata? = null
) {
    data class Prediction(
        val bytesBase64Encoded: String?,
        val mimeType: String?
    )

    data class Metadata(
        val tokenMetadata: TokenMetadata? = null
    )

    data class TokenMetadata(
        val inputTokenCount: InputTokenCount? = null,
        val outputTokenCount: OutputTokenCount? = null
    )

    data class InputTokenCount(
        val totalTokens: Int? = null,
        val totalBillableCharacters: Int? = null
    )

    data class OutputTokenCount(
        val totalTokens: Int? = null
    )
}