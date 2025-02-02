package app.users.api.security

import app.users.api.Constants.AUTHORIZATION_HEADER
import app.users.api.Constants.BEARER_START_WITH
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component("jwtFilter")
class JwtFilter(private val context: ApplicationContext) : WebFilter {

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> = exchange
        .request
        .run(::resolveToken)
        .run {
            chain.run {
                context.getBean<SecurityService>().run {
                    when {
                        !isNullOrBlank() && run(::validateToken) ->
                            run(::getAuthentication)
                                .run(::withAuthentication)
                                .run(filter(exchange)::contextWrite)

                        else -> filter(exchange)
                    }
                }
            }
        }

    private fun resolveToken(request: ServerHttpRequest): String? = request
        .headers
        .getFirst(AUTHORIZATION_HEADER)
        .apply {
            return when {
                !isNullOrBlank() &&
                        startsWith(BEARER_START_WITH) -> substring(startIndex = 7)

                else -> null
            }
        }
}