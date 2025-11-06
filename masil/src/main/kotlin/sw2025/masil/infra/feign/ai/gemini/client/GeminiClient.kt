package sw2025.masil.infra.feign.ai.gemini.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import sw2025.masil.infra.feign.ai.gemini.dto.req.GeminiPromptRequestDto
import sw2025.masil.infra.feign.ai.gemini.dto.res.GeminiResponse

@FeignClient(
    name = "geminiClient",
    url = "https://generativelanguage.googleapis.com/v1"
)
interface GeminiClient {

    @PostMapping("/models/{model}:generateContent")
    fun generateText(
        @PathVariable model: String,
        @RequestParam("key") apiKey: String,
        @RequestBody body: GeminiPromptRequestDto
    ): GeminiResponse

    @PostMapping("/models/{model}:generateContent")
    fun generateImage(
        @PathVariable model: String,
        @RequestParam("key") apiKey: String,
        @RequestBody body: GeminiPromptRequestDto
    ): GeminiResponse
}
