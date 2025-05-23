package app.users.api.security

import app.users.api.Constants.ROLE_ADMIN
import app.users.api.Loggers.d
import app.users.api.Properties
import app.users.api.web.Web.SpaWebFilter
import app.users.password.UserReset.EndPoint.API_CHANGE_PASSWORD_PATH
import app.users.password.UserReset.EndPoint.API_RESET_PASSWORD_FINISH_PATH
import app.users.password.UserReset.EndPoint.API_RESET_PASSWORD_INIT_PATH
import app.users.signup.Signup.EndPoint.API_ACTIVATE_PATH
import app.users.signup.Signup.EndPoint.API_SIGNUP_PATH
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.HTTP_BASIC
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ContentSecurityPolicyServerHttpHeadersWriter.CONTENT_SECURITY_POLICY
import org.springframework.security.web.server.header.FeaturePolicyServerHttpHeadersWriter.FEATURE_POLICY
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.WebFilter

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration(private val context: ApplicationContext) {
    companion object {
        val permitAll = arrayOf(
            "/",
            "/*.*",
            "/api/user/authenticate",
            API_SIGNUP_PATH,
            API_ACTIVATE_PATH,
            API_RESET_PASSWORD_INIT_PATH,
            API_RESET_PASSWORD_FINISH_PATH,
            "/api/ai/*",
        )
        val authenticated = arrayOf(
            "/api/**",
            "/services/**",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/api/auth-info",
            API_CHANGE_PASSWORD_PATH,
            "/api/users/**",
        )
        val adminAuthority = arrayOf(
            "/management/info",
            "/management/prometheus",
            "/management/health",
            "/management/health/**",
            "/management/**",
            "/api/admin/**",
        )
        val negated = arrayOf(
            "/app/**",
            "/i18n/**",
            "/content/**",
            "/swagger-ui/**",
            "/test/**",
            "/webjars/**"
        )
        val spaNegated = arrayOf(
            "/api",
            "/management",
            "/services",
            "/swagger",
            "/v2/api-docs",
        )

    }

    @Bean("passwordEncoder")
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun reactiveAuthenticationManager(): ReactiveAuthenticationManager = context
        .getBean<ReactiveUserDetailsService>()
        .run(::UserDetailsRepositoryReactiveAuthenticationManager)
        .apply { setPasswordEncoder(passwordEncoder()) }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http.securityMatcher(
            NegatedServerWebExchangeMatcher(
                OrServerWebExchangeMatcher(
                    pathMatchers(
                        "/app/**",
                        "/i18n/**",
                        "/content/**",
                        "/swagger-ui/**",
                        "/test/**",
                        "/webjars/**"
                    ), pathMatchers(OPTIONS, "/**")
                )
            )
        ).csrf { csrf ->
            csrf.disable()
                .addFilterAt(SpaWebFilter(), AUTHENTICATION)
                .addFilterAt(JwtFilter(context), HTTP_BASIC)
                .authenticationManager(reactiveAuthenticationManager())
                .exceptionHandling { }
                .headers { h ->
                    h.contentSecurityPolicy { p ->
                        p.policyDirectives(CONTENT_SECURITY_POLICY)
                    }
                    h.referrerPolicy { p -> p.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                    h.permissionsPolicy { p -> p.policy(FEATURE_POLICY) }
                    h.frameOptions { f -> f.disable() }
                }.authorizeExchange {
                    it.pathMatchers(*permitAll).permitAll()
                    it.pathMatchers(*authenticated).authenticated()
                    it.pathMatchers(*adminAuthority).hasAuthority(ROLE_ADMIN)
                }
        }.build()

    @Bean
    fun corsFilter(): WebFilter = CorsWebFilter(UrlBasedCorsConfigurationSource().apply source@{
        context.getBean<Properties>().cors.apply config@{
            when {
                allowedOrigins != null && allowedOrigins!!.isNotEmpty() -> this@source.apply {
                    d("Registering CORS filter")
                    setOf(
                        "/api/**",
                        "/management/**",
                        "/v2/api-docs"
                    ).forEach { registerCorsConfiguration(it, this@config) }
                }
            }
        }
    })
}