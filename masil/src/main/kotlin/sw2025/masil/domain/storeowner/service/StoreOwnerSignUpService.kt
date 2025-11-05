package sw2025.masil.domain.storeowner.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import sw2025.masil.domain.storeowner.exception.DuplicationAccountIdException
import sw2025.masil.domain.storeowner.persistence.entity.StoreOwner
import sw2025.masil.domain.storeowner.persistence.repository.StoreOwnerRepository
import sw2025.masil.domain.storeowner.presentation.dto.req.StoreOwnerSignUpRequest
import java.util.UUID

@Service
class StoreOwnerSignUpService(
    private val storeOwnerRepository: StoreOwnerRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun execute(request: StoreOwnerSignUpRequest): UUID {
        if (storeOwnerRepository.existsByAccountId(request.accountId)) {
            throw DuplicationAccountIdException
        }

        val storeOwner = storeOwnerRepository.save(
            StoreOwner(
                name = request.name,
                password = passwordEncoder.encode(request.password),
                accountId = request.accountId
            )
        )

        return storeOwner.id
    }
}
