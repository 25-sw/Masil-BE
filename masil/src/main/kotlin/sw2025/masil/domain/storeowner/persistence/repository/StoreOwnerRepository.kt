package sw2025.masil.domain.storeowner.persistence.repository

import org.springframework.data.repository.CrudRepository
import sw2025.masil.domain.storeowner.persistence.entity.StoreOwner
import java.util.*

interface StoreOwnerRepository : CrudRepository<StoreOwner, UUID> {
    fun existsByAccountId(accountId: String): Boolean
    fun findByAccountId(accountId: String): StoreOwner?
}
