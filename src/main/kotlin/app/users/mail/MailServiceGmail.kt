package app.users.mail

import app.core.Constants.GMAIL
import app.core.Loggers.i
import app.core.Properties
import app.users.mail.MailService.AbstractThymeleafMailService
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine

@Suppress("unused")
@Async
@Service
@Profile(GMAIL)
class MailServiceGmail(
    private val properties: Properties,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) : AbstractThymeleafMailService(
    properties,
    messageSource,
    templateEngine
) {
    override fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) = i(MailServiceGmail::class.java.name)
}