package sw2025.masil.domain.poster.presentation.dto.req

data class CreatePosterRequest(
    val storeName: String,
    val serviceName: String,
    val category: String,
    val targetAudience: String,
)
