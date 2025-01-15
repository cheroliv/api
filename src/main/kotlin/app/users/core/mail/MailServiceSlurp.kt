@file:Suppress("CanBeParameter","unused")

package app.users.core.mail

import app.users.core.Constants.MAILSLURP
import app.users.core.Loggers.i
import app.users.core.Properties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine

@Async
@Service
@Profile(MAILSLURP)
class MailServiceSlurp(
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
    ) = i(MailServiceSlurp::class.java.name)
}