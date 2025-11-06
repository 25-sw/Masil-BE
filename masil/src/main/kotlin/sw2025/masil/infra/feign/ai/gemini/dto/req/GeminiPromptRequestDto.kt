package sw2025.masil.infra.feign.ai.gemini.dto.req

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiPromptRequestDto(
    val contents: List<Content>,
    // 바디 루트 키는 camelCase "generationConfig" 가 맞습니다.
    @JsonProperty("generationConfig")
    val generationConfig: GenerationConfig? = null
) {
    data class Content(
        val role: String? = null,
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GenerationConfig(
        // 아래 필드들은 wire-format 이 snake_case 여야 합니다.
        @JsonProperty("response_mime_type")
        val responseMimeType: String? = null,

        @JsonProperty("temperature")
        val temperature: Double? = null,

        @JsonProperty("top_p")
        val topP: Double? = null,

        @JsonProperty("top_k")
        val topK: Int? = null,

        @JsonProperty("max_output_tokens")
        val maxOutputTokens: Int? = null
    )
}