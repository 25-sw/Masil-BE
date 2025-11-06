package sw2025.masil.infra.feign.ai.gemini.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import sw2025.masil.infra.feign.ai.gemini.dto.req.GeminiImageRequest
import sw2025.masil.infra.feign.ai.gemini.dto.res.GeminiImageResponse

@FeignClient(
    name = "geminiImageClient",
    url = "https://generativelanguage.googleapis.com/v1beta"
)
interface GeminiImageClient {

    @PostMapping("/models/{model}:generateContent")
    fun generateContent(
        @PathVariable model: String, // "gemini-2.5-flash-image"
        @RequestHeader("x-goog-api-key") apiKey: String, // 헤더로 전달
        @RequestBody body: GeminiImageRequest
    ): GeminiImageResponse
}