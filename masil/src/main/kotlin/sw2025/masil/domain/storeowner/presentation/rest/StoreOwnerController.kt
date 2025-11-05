package sw2025.masil.domain.storeowner.presentation.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sw2025.masil.domain.storeowner.presentation.dto.req.StoreOwnerLoginRequest
import sw2025.masil.domain.storeowner.presentation.dto.req.StoreOwnerSignUpRequest
import sw2025.masil.domain.storeowner.service.StoreOwnerLoginService
import sw2025.masil.domain.storeowner.service.StoreOwnerSignUpService

@RestController
@RequestMapping("/store-owner")
class StoreOwnerController(
    private val storeOwnerLoginService: StoreOwnerLoginService,
    private val storeOwnerSignUpService: StoreOwnerSignUpService
) {
    @PostMapping("/login")
    fun login(@RequestBody request: StoreOwnerLoginRequest) =
        storeOwnerLoginService.execute(request)

    @PostMapping("/sign-up")
    fun signUp(@RequestBody request: StoreOwnerSignUpRequest) =
        storeOwnerSignUpService.execute(request)

    @GetMapping("/test")
    fun signUp() {
        println("아제발")
    }
}
