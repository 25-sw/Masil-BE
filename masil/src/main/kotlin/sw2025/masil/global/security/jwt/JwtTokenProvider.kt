package sw2025.masil.global.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import sw2025.masil.global.security.auth.AuthDetailsService
import sw2025.masil.global.security.jwt.exception.ExpiredJwtException
import sw2025.masil.global.security.jwt.exception.InvaildJwtException
import sw2025.masil.global.security.jwt.tokenResponse.TokenResponse
import java.util.*

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
    private val authDetailsService: AuthDetailsService
) {
    companion object {
        private const val ACCESS_KEY = "access_token"
    }

    fun generateToken(userId: String): TokenResponse {
        val accessToken = generateAccessToken(userId, ACCESS_KEY, jwtProperties.accessExp)
        return TokenResponse(accessToken)
    }

    private fun generateAccessToken(
        id: String,
        type: String,
        exp: Long
    ): String =
        Jwts.builder()
            .setSubject(id)
            .setHeaderParam("typ", type)
            .signWith(SignatureAlgorithm.HS256, jwtProperties.secretKey)
            .setExpiration(Date(System.currentTimeMillis() + exp * 1000))
            .setIssuedAt(Date())
            .compact()

    fun resolveToken(request: jakarta.servlet.http.HttpServletRequest): String? =
        request.getHeader(jwtProperties.header)?.also {
            if (it.startsWith(jwtProperties.prefix)) {
                return it.substring(jwtProperties.prefix.length)
            }
        }

    fun authentication(token: String): Authentication? {
        val body: Claims = getJws(token).body
        val userDetails: UserDetails = getDetails(body)
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    private fun getJws(token: String): Jws<Claims> {
        return try {
            Jwts.parser().setSigningKey(jwtProperties.secretKey).parseClaimsJws(token)
        } catch (e: ExpiredJwtException) {
            throw ExpiredJwtException
        } catch (e: Exception) {
            throw InvaildJwtException
        }
    }

    private fun getDetails(body: Claims): UserDetails {
        return authDetailsService.loadUserByUsername(body.subject)
    }
}
