package sw2025.masil.domain.poster.service

import org.springframework.stereotype.Service
import sw2025.masil.domain.poster.persistence.repository.PosterRepository
import sw2025.masil.domain.poster.presentation.dto.res.GetPosterByCurrentUserResponse
import sw2025.masil.domain.storeowner.facade.StoreOwnerFacade

@Service
class FindPostersByCurrentUserService(
    private val storeOwnerFacade: StoreOwnerFacade,
    private val posterRepository: PosterRepository
) {
    fun execute(): List<GetPosterByCurrentUserResponse> {
        val storeOwner = storeOwnerFacade.currentStoreOwner()
        val posterList = posterRepository.findAllByStoreOwner(storeOwner)
        return posterList.map { GetPosterByCurrentUserResponse(it.id.toString(), it.fileName) }
    }
}