package app.users.core.security

import app.users.core.Constants.AUTHORITIES_KEY
import app.users.core.Constants.INVALID_TOKEN
import app.users.core.Constants.VALID_TOKEN
import app.users.core.Loggers
import app.users.core.Loggers.d
import app.users.core.Loggers.i
import app.users.core.Loggers.t
import app.users.core.Properties
import app.users.core.dao.UserDao.userDetails
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts.builder
import io.jsonwebtoken.Jwts.parserBuilder
import io.jsonwebtoken.SignatureAlgorithm.HS512
import io.jsonwebtoken.io.Decoders.BASE64
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.lang.Strings.hasLength
import io.jsonwebtoken.security.Keys.hmacShaKeyFor
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.security.Key
import java.time.ZonedDateTime.now
import java.util.*

@Service("userDetailsService")
class SecurityService(
    private val context: ApplicationContext,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private var key: Key? = null,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private var tokenValidityInMilliseconds: Long = 0,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private var tokenValidityInMillisecondsForRememberMe: Long = 0
) : ReactiveUserDetailsService, InitializingBean {

    @Transactional(readOnly = true)
    @Throws(UsernameNotFoundException::class)
    override fun findByUsername(emailOrLogin: String): Mono<UserDetails> =
        context.userDetails(emailOrLogin)

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        context.getBean<Properties>()
            .security
            .authentication
            .jwt
            .secret
            .run {
                key = hmacShaKeyFor(
                    when {
                        !hasLength(this) -> (
                                buildString {
                                    append("Warning: the Jwt key used is not Base64-encoded. ")
                                    append("We recommend using the `school.security.authentication.jwt.base64-secret`")
                                    append(" key for optimum security.")
                                }
                                ).run(Loggers::w)
                            .run { toByteArray() }

                        else -> d("Using a Base64-encoded Jwt secret key").run {
                            context.getBean<Properties>()
                                .security
                                .authentication
                                .jwt
                                .base64Secret
                                .run(BASE64::decode)
                        }
                    }
                )
            }
        tokenValidityInMilliseconds = context.getBean<Properties>()
            .security
            .authentication
            .jwt
            .tokenValidityInSeconds * 1000
        tokenValidityInMillisecondsForRememberMe = context.getBean<Properties>()
            .security
            .authentication
            .jwt
            .tokenValidityInSecondsForRememberMe * 1000
    }

    private fun calculateExpirationDate(rememberMe: Boolean): Date = when {
        rememberMe -> tokenValidityInMillisecondsForRememberMe

        else -> tokenValidityInMilliseconds
    }.run {
        1000.run(::div)
            .run(now()::plusSeconds)
            .toInstant()
            .run(Date::from)
    }


    suspend fun createToken(
        authentication: Authentication,
        rememberMe: Boolean
    ): String = now().run {
        builder().setSubject(authentication.name)
            .claim(
                AUTHORITIES_KEY,
                authentication.authorities
                    .asSequence()
                    .map { it.authority }
                    .joinToString(separator = ","))
            .signWith(key, HS512)
            .setExpiration(calculateExpirationDate(rememberMe))
            .serializeToJsonWith(JacksonSerializer())
            .compact()
    }

    fun getAuthentication(token: String): Authentication {
        parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .apply {
                this[AUTHORITIES_KEY]
                    .toString()
                    .splitToSequence(",")
                    .mapTo(mutableListOf()) { SimpleGrantedAuthority(it) }
                    .apply authorities@{
                        return@getAuthentication UsernamePasswordAuthenticationToken(
                            User(subject, "", this@authorities),
                            token,
                            this@authorities
                        )
                    }
            }
    }

    fun validateToken(token: String): Boolean = try {
        parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
        VALID_TOKEN
    } catch (e: JwtException) {
        i("Invalid Jwt token.")
        t("Invalid Jwt token trace. $e")
        INVALID_TOKEN
    } catch (e: IllegalArgumentException) {
        i("Invalid Jwt token.")
        t("Invalid Jwt token trace. $e")
        INVALID_TOKEN
    }
}