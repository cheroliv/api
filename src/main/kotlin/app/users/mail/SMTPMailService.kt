package app.users.mail

import app.users.core.Loggers.d
import app.users.core.Loggers.w
import app.users.core.Properties
import jakarta.mail.MessagingException
import org.springframework.context.MessageSource
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine
import kotlin.text.Charsets.UTF_8

@Async
@Service
class SMTPMailService(
    private val properties: Properties,
    private val mailSender: JavaMailSender,
    messageSource: MessageSource,
    templateEngine: SpringTemplateEngine
) : AbstractThymeleafMailTemplatingService(properties, messageSource, templateEngine) {
    override fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) = mailSender.createMimeMessage().run {
        try {
            MimeMessageHelper(this, isMultipart, UTF_8.name()).apply {
                setTo(to)
                setFrom(properties.mail.from)
                setSubject(subject)
                setText(content, isHtml)
            }
            mailSender.send(this)
            d("Sent email to User '$to'")
        } catch (e: MailException) {
            w("Email could not be sent to user '$to'", e)
        } catch (e: MessagingException) {
            w("Email could not be sent to user '$to'", e)
        }
    }
}