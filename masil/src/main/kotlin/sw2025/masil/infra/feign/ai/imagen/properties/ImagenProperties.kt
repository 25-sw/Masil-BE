package sw2025.masil.infra.feign.ai.imagen.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.imagen")
class ImagenProperties(
    val projectId: String,
    val location: String = "us-central1",
    val accessToken: String,
)