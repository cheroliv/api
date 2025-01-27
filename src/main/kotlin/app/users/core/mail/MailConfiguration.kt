@file:Suppress("unused")

package app.users.core.mail

import app.users.core.Constants.MAIL_DEBUG
import app.users.core.Constants.MAIL_SMTP_AUTH
import app.users.core.Constants.MAIL_TRANSPORT_PROTOCOL
import app.users.core.Constants.MAIL_TRANSPORT_STARTTLS_ENABLE
import app.users.core.Properties
import app.users.core.Utils.privateProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class MailConfiguration(
    private val properties: Properties,
    private val context: ApplicationContext
) {
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
    fun javaMailSender(): JavaMailSender = JavaMailSenderImpl().apply {
        privateProperties.apply {
            host = get("google.test.mail.host").toString()
            port = get("google.test.mail.port").toString().toInt()
            username = get("google.test.mail").toString()
            password = get("google.test.app.app-password").toString()
            mapOf(
                MAIL_SMTP_AUTH to properties.mail.property.smtp.auth,
                MAIL_TRANSPORT_STARTTLS_ENABLE to properties.mail.property.smtp.starttls.enable,
                MAIL_TRANSPORT_PROTOCOL to properties.mail.property.transport.protocol,
                MAIL_DEBUG to properties.mail.property.debug,
                "spring.mail.test-connection" to true,
            ).forEach { javaMailProperties[it.key] = it.value }
        }
    }
}