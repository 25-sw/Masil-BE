package sw2025.masil.infra.feign.ai.imagen.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import sw2025.masil.infra.feign.ai.imagen.dto.req.ImagenPromptRequest
import sw2025.masil.infra.feign.ai.imagen.dto.res.ImagenResponse

@FeignClient(
    name = "imagen-client",
    url = "https://us-central1-aiplatform.googleapis.com"
)
interface ImagenClient {
    @PostMapping(
        value = ["/v1/projects/{projectId}/locations/{location}/publishers/google/models/imagen-3.0-generate-001:predict"],
        headers = ["Content-Type=application/json"]
    )
    fun generateImage(
        @PathVariable("projectId") projectId: String,
        @PathVariable("location") location: String,
        @RequestHeader("Authorization") accessToken: String,  // "Bearer YOUR_TOKEN"
        @RequestBody body: ImagenPromptRequest
    ): ImagenResponse
}