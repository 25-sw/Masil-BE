package sw2025.masil.domain.storeowner.presentation.dto.req

data class StoreOwnerLoginRequest(
    val accountId: String,
    val password: String
)
