package sw2025.masil.global.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtTokenFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {
    private val excludedPaths = listOf("/store-owner/sign-up", "/store-owner/login")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = jwtTokenProvider.resolveToken(request)

        token?.run {
            SecurityContextHolder.getContext().authentication = jwtTokenProvider.authentication(token)
        }

        filterChain.doFilter(request, response)
    }
}
