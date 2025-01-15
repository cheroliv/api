package app.users.core.mail

import app.users.core.Constants.GMAIL
import app.users.core.Constants.MAILSLURP
import app.users.core.Constants.MAIL_DEBUG
import app.users.core.Constants.MAIL_SMTP_AUTH
import app.users.core.Constants.MAIL_TRANSPORT_PROTOCOL
import app.users.core.Constants.MAIL_TRANSPORT_STARTTLS_ENABLE
import app.users.core.Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class MailConfiguration(private val properties: Properties) {
    data class GoogleAuthConfig(
        val clientId: String,
        val projectId: String,
        val authUri: String,
        val tokenUri: String,
        val authProviderX509CertUrl: String,
        val clientSecret: String,
        val redirectUris: List<String>
    )
    @Bean
    @Profile("!$MAILSLURP & !$GMAIL")
    fun javaMailSender(): JavaMailSender = JavaMailSenderImpl().apply {
        host = properties.mail.host
        port = properties.mail.port
        username = properties.mail.from
        password = properties.mail.password
        mapOf(
            MAIL_TRANSPORT_PROTOCOL to properties.mail.property.transport.protocol,
            MAIL_SMTP_AUTH to properties.mail.property.smtp.auth,
            MAIL_TRANSPORT_STARTTLS_ENABLE to properties.mail.property.smtp.starttls.enable,
            MAIL_DEBUG to properties.mail.property.debug,
            "spring.mail.test-connection" to true,
            "mail.smtp.ssl.trust" to true,
            "mail.connect_timeout" to 60000,
            "mail.auth_api_key" to "",
        ).forEach { javaMailProperties[it.key] = it.value }
    }
}