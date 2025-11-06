package sw2025.masil.infra.feign.ai.gemini.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.gemini")
class GeminiProperties(
    val apiKey: String,
    val model: String
)