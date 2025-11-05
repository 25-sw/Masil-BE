package sw2025.masil.domain.storeowner.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import sw2025.masil.domain.storeowner.exception.PasswordMismatchException
import sw2025.masil.domain.storeowner.exception.StoreOwnerNotFoundException
import sw2025.masil.domain.storeowner.persistence.repository.StoreOwnerRepository
import sw2025.masil.domain.storeowner.presentation.dto.req.StoreOwnerLoginRequest
import sw2025.masil.global.security.jwt.JwtTokenProvider
import sw2025.masil.global.security.jwt.tokenResponse.TokenResponse

@Service
class StoreOwnerLoginService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val storeOwnerRepository: StoreOwnerRepository
) {
    fun execute(request: StoreOwnerLoginRequest): TokenResponse {
        val storeOwner = storeOwnerRepository.findByAccountId(request.accountId) ?: throw StoreOwnerNotFoundException

        if (!passwordEncoder.matches(request.password, storeOwner.password)) {
            throw PasswordMismatchException
        }

        return TokenResponse(storeOwner.accountId)
    }
}
