package sw2025.masil.global.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import sw2025.masil.global.security.auth.AuthDetailsService
import sw2025.masil.global.security.jwt.exception.ExpiredJwtException
import sw2025.masil.global.security.jwt.exception.InvaildJwtException
import sw2025.masil.global.security.jwt.tokenResponse.TokenResponse
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
    private val authDetailsService: AuthDetailsService
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray())

    companion object {
        private const val ACCESS_KEY = "access_token"
    }

    fun generateToken(userId: String): TokenResponse {
        return TokenResponse(generateAccessToken(userId, ACCESS_KEY, jwtProperties.accessExp))
    }

    private fun generateAccessToken(
        id: String,
        type: String,
        exp: Long
    ): String =
        Jwts
            .builder()
            .subject(id)
            .claim("type", type)
            .signWith(secretKey)
            .issuedAt(Date()) // 발행 시간 설정
            .expiration(Date(System.currentTimeMillis() + exp * 1000))
            .compact()

    fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader(jwtProperties.header)?.also {
            if (it.startsWith(jwtProperties.prefix)) {
                return it.substring(jwtProperties.prefix.length).trim()
            }
        }

    fun authentication(token: String): Authentication? {
        val userDetails: UserDetails = getDetails(getJws(token))
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    private fun getJws(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw ExpiredJwtException
        } catch (e: Exception) {
            throw InvaildJwtException
        }

    private fun getDetails(body: Claims): UserDetails = authDetailsService.loadUserByUsername(body.subject)
}
