package sw2025.masil.domain.notification.presentation.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sw2025.masil.domain.notification.enum.IndustryType
import sw2025.masil.domain.notification.enum.Region
import sw2025.masil.domain.notification.enum.RelatedInstitution
import sw2025.masil.domain.notification.enum.SupportTarget
import sw2025.masil.domain.notification.service.NotificationService

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {
    @GetMapping()
    fun notification(
        @RequestParam industryList: List<IndustryType>?,
        @RequestParam regionList: List<Region>?,
        @RequestParam relatedInstitutionList: List<RelatedInstitution>?,
        @RequestParam supportTargetList: List<SupportTarget>?
    ) = notificationService.execute(industryList, regionList, relatedInstitutionList, supportTargetList)
}
