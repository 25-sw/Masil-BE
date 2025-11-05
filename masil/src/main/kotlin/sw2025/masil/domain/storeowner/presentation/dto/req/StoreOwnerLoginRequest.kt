package sw2025.masil.domain.storeowner.presentation.dto.req

data class StoreOwnerLoginRequest(
    val username: String,
    val accountId: String,
    val password: String
)
