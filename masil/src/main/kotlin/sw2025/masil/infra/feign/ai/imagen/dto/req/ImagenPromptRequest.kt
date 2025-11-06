package sw2025.masil.infra.feign.ai.imagen.dto.req

data class ImagenPromptRequest(
    val instances: List<Instance>,
    val parameters: Parameters
) {
    data class Instance(
        val prompt: String
    )

    data class Parameters(
        val sampleCount: Int = 1,
        val aspectRatio: String = "9:16",  // 1:1, 3:4, 4:3, 9:16, 16:9
        val negativePrompt: String? = null,
        val safetySetting: String = "block_some"  // block_some, block_few, block_most
    )
}