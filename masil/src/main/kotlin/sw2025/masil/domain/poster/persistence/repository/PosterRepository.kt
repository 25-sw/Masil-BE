package sw2025.masil.domain.poster.persistence.repository

import org.springframework.data.repository.CrudRepository
import sw2025.masil.domain.poster.persistence.entity.Poster
import sw2025.masil.domain.storeowner.persistence.entity.StoreOwner
import java.util.*

interface PosterRepository : CrudRepository<Poster, UUID> {
    fun findAllByStoreOwner(storeOwner: StoreOwner) : List<Poster>;
}