package sw2025.masil.infra.feign.ai.gemini.dto.res

data class GeminiResponse(
    val candidates: List<Candidate> = emptyList()
) {
    data class Candidate(
        val content: Content
    )
    data class Content(
        val parts: List<Part>
    )
    data class Part(
        val text: String? = null,
        val inlineData: InlineData? = null
    )
    data class InlineData(
        val mimeType: String,
        val data: String
    )
}