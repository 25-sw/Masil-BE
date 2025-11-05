package sw2025.masil.domain.storeowner.presentation.dto.req

data class StoreOwnerSignUpRequest(
    val name: String,
    val accountId: String,
    val password: String
)
