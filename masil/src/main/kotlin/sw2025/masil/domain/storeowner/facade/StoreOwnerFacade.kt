package sw2025.masil.domain.storeowner.facade

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import sw2025.masil.domain.storeowner.exception.StoreOwnerNotFoundException
import sw2025.masil.domain.storeowner.persistence.entity.StoreOwner
import sw2025.masil.domain.storeowner.persistence.repository.StoreOwnerRepository
import java.util.UUID

@Component
class StoreOwnerFacade(
    private val storeOwnerRepository: StoreOwnerRepository
) {
    fun currentStoreOwner(): StoreOwner {
        val storeOwnerId = SecurityContextHolder.getContext().authentication.name
        return storeOwnerRepository.findByAccountId(storeOwnerId) ?: throw StoreOwnerNotFoundException
    }

    fun findByIdOrThrow(storeOwnerId: UUID) =
        storeOwnerRepository.findById(storeOwnerId) ?: throw StoreOwnerNotFoundException
}
