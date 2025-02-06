package app.users.api

import app.users.api.Constants.SMTP
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
import org.springframework.web.cors.CorsConfiguration

@PropertySources(
    PropertySource("classpath:git.properties", ignoreResourceNotFound = true),
    PropertySource("classpath:META-INF/build-info.properties", ignoreResourceNotFound = true)
)
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
class Properties @ConstructorBinding constructor(
    val message: String = "",
    val item: String,
    val goVisitMessage: String,
    val clientApp: ClientApp = ClientApp(),
    val database: Database = Database(),
    val mailbox: MailBox = MailBox(),
    val http: Http = Http(),
    val cache: Cache = Cache(),
    val security: Security = Security(),
    val cors: CorsConfiguration = CorsConfiguration(),
) {
    class MailBox(
        val noReply: MailAccount = MailAccount(),
//        val contact: Mail = Mail(),
//        val job: Mail = Mail(),
//        val newsletter: Mail = Mail(),
    ) {
        class MailAccount(
            val name: String = "",
            val from: String = "",
            val password: String = "",
            val host: String = "",
            val port: Int = 587,
            val properties: MailProperties = MailProperties(),
            val baseUrl: String = "",
        ) {
            class MailProperties(
                val debug: Boolean = false,
                val transfer: MailTransfer = MailTransfer(),
                val transport: Transport = Transport(),
            ) {
                class Transport(val protocol: String = SMTP)
                class MailTransfer(
                    val auth: Boolean = false,
                    val starttls: Starttls = Starttls()
                ) {
                    class Starttls(val enable: Boolean = false)
                }
            }
        }
    }


    class ClientApp(val name: String = "")
    class Database(val populatorPath: String = "")


    class Http(val cache: Cache = Cache()) {
        class Cache(val timeToLiveInDays: Int = 1461)
    }

    class Cache(val ehcache: Ehcache = Ehcache()) {
        class Ehcache(
            val timeToLiveSeconds: Int = 3600,
            val maxEntries: Long = 100
        )
    }

    class Security(
        val rememberMe: RememberMe = RememberMe(),
        val authentication: Authentication = Authentication(),
        val clientAuthorization: ClientAuthorization = ClientAuthorization()
    ) {
        class RememberMe(var key: String = "")

        class Authentication(val jwt: Jwt = Jwt()) {
            class Jwt(
                val tokenValidityInSecondsForRememberMe: Long = 2592000,
                val tokenValidityInSeconds: Long = 1800,
                var base64Secret: String = "",
                var secret: String = ""
            )
        }

        class ClientAuthorization(
            var accessTokenUri: String = "",
            var tokenServiceId: String = "",
            var clientId: String = "",
            var clientSecret: String = ""
        )
    }
}