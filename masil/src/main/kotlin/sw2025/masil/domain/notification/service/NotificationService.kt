package sw2025.masil.domain.notification.service

import org.springframework.stereotype.Service
import sw2025.masil.domain.notification.enum.IndustryType
import sw2025.masil.domain.notification.enum.Region
import sw2025.masil.domain.notification.enum.RelatedInstitution
import sw2025.masil.domain.notification.enum.SupportTarget
import sw2025.masil.domain.notification.presentation.dto.req.SearchRequestDto
import sw2025.masil.domain.notification.presentation.dto.res.PbancItem
import sw2025.masil.infra.feign.notification.NotificationFeignClient

@Service
class NotificationService(
    private val notificationFeignClient: NotificationFeignClient
) {
    fun execute(
        industryList: List<IndustryType>?,
        regionList: List<Region>?,
        relatedInstitutionList: List<RelatedInstitution>?,
        supportTargetList: List<SupportTarget>?
    ): List<PbancItem> {
        val response = notificationFeignClient.getNotification(SearchRequestDto())
        return response.data!!.defaultBlock!!.list
    }
}
