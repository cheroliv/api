package app.users.api

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
    val mail: Mail = Mail(),
    val mailbox: MailBox = MailBox(),
    val http: Http = Http(),
    val cache: Cache = Cache(),
    val security: Security = Security(),
    val cors: CorsConfiguration = CorsConfiguration(),
) {
    //    text_to_add="# google
    //    app.mail.no-reply.host=smtp.gmail.com
    //    app.mail.no-reply.port=587
    //
    //    # google account test
    //    app.mail.no-reply.email=tester@gmail.com
    //    app.mail.no-reply.password=sxckqebcmaimwfvl"
    class Mail(
        val name: String = "",
        val token: String = "",
        val enabled: Boolean = false,
        val from: String = "",
        val baseUrl: String = "",
        val host: String = "",
        val port: Int = -1,
        val password: String = "",
        val property: SmtpProperty = SmtpProperty(),
    ) {
        class SmtpProperty(
            val debug: Boolean = false,
            val transport: Transport = Transport(),
            val smtp: Smtp = Smtp()
        ) {
            class Transport(val protocol: String = "")
            class Smtp(
                val auth: Boolean = false,
                val starttls: Starttls = Starttls()
            ) {
                class Starttls(val enable: Boolean = false)
            }
        }
    }

    class MailBox(
        val noReply: MailAccount = MailAccount(),
//        val contact: Mail = Mail(),
//        val recrutment: Mail = Mail(),
    ) {
        class MailAccount(
            val from: String = "",
            val password: String = "",
            val baseUrl: String = "",
            val name: String = "no-reply",
            val host: String = "",
            val port: Int = -1,
            val enabled: Boolean = false,
            val smptProperties: SmtpProperties = SmtpProperties(),
            val imapsProperties: ImapsProperties = ImapsProperties(),
        ) {
            class SmtpProperties(
                val debug: Boolean = false,
                val smtp: Smtp = Smtp()
            ) {
                class Smtp(
                    val auth: Boolean = false,
                    val starttls: Starttls = Starttls()
                ) {
                    class Starttls(val enable: Boolean = false)
                }
            }

            class ImapsProperties(
                val debug: Boolean = false,
                val imaps: Imaps = Imaps()
            ) {
                class Imaps(
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