package sw2025.masil.global.security.auth

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import sw2025.masil.domain.storeowner.facade.StoreOwnerFacade

@Component
class AuthDetailsService(
    private val storeOwnerFacade: StoreOwnerFacade
) : UserDetailsService {
    override fun loadUserByUsername(accountId: String): UserDetails {
        return AuthDetails(accountId)
    }
}
