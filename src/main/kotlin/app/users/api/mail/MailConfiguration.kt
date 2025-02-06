package app.users.api.mail

import app.users.api.Constants.MAIL_DEBUG
import app.users.api.Constants.MAIL_SMTP_AUTH
import app.users.api.Constants.MAIL_TRANSPORT_PROTOCOL
import app.users.api.Constants.MAIL_TRANSPORT_STARTTLS_ENABLE
import app.users.api.Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    fun noReply(): JavaMailSender = JavaMailSenderImpl().apply {
        username = properties.mailbox.noReply.from
        password = properties.mailbox.noReply.password
        host = properties.mailbox.noReply.host
        port = properties.mailbox.noReply.port
        mapOf(
            MAIL_SMTP_AUTH to properties.mailbox.noReply.properties.transfer.auth,
            MAIL_TRANSPORT_STARTTLS_ENABLE to properties.mailbox.noReply.properties.transfer.starttls.enable,
            MAIL_TRANSPORT_PROTOCOL to properties.mailbox.noReply.properties.transport.protocol,
            MAIL_DEBUG to properties.mailbox.noReply.properties.debug,
            "spring.mail.test-connection" to true,
        ).forEach { javaMailProperties[it.key] = it.value }
    }
}
