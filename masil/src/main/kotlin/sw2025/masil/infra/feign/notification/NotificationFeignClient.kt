package sw2025.masil.infra.feign.notification

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import sw2025.masil.domain.notification.presentation.dto.req.SearchRequestDto
import sw2025.masil.domain.notification.presentation.dto.res.CombinePbancApiResponse
import sw2025.masil.global.config.SbizFeignConfig

@FeignClient(
    name = "sbiz",
    url = "https://www.sbiz24.kr",
    configuration = [SbizFeignConfig::class]
)
interface NotificationFeignClient {

    @PostMapping(
        value = ["/api/combinePbanc/list"],
        consumes = ["application/json;charset=UTF-8"],
        produces = ["application/json"]
    )
    fun getNotification(@RequestBody request: SearchRequestDto): CombinePbancApiResponse
}
