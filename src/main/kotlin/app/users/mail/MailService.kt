package app.users.mail

import app.core.Constants.BASE_URL
import app.core.Constants.TEMPLATE_NAME_CREATION
import app.core.Constants.TEMPLATE_NAME_PASSWORD
import app.core.Constants.TEMPLATE_NAME_SIGNUP
import app.core.Constants.TITLE_KEY_PASSWORD
import app.core.Constants.TITLE_KEY_SIGNUP
import app.core.Constants.USER
import app.core.Loggers.d
import app.core.Properties
import app.users.User
import org.springframework.context.MessageSource
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.util.Locale.forLanguageTag


interface MailService {
    fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    )

    fun sendEmailFromTemplate(
        account: User,
        templateName: String,
        titleKey: String
    )

    fun sendPasswordResetMail(account: User)
    fun sendActivationEmail(account: User)
    fun sendCreationEmail(account: User)

    abstract class AbstractThymeleafMailService(
        private val properties: Properties,
        private val messageSource: MessageSource,
        private val templateEngine: SpringTemplateEngine
    ) : MailService {

        abstract override fun sendEmail(
            to: String,
            subject: String,
            content: String,
            isMultipart: Boolean,
            isHtml: Boolean
        )

        override fun sendEmailFromTemplate(
            account: User,
            templateName: String,
            titleKey: String
        ) {
            @Suppress(
                "SENSELESS_NULL_IN_WHEN"
            )
            when (account.email) {
                null -> {
                    d("Email doesn't exist for user '${account.login}'")
                    return
                }

                else -> forLanguageTag(account.langKey).apply {
                    sendEmail(
                        account.email,
                        messageSource.getMessage(titleKey, null, this),
                        templateEngine.process(templateName, Context(this).apply {
                            setVariable(USER, account)
                            setVariable(BASE_URL, properties.mail.baseUrl)
                        }),
                        isMultipart = false,
                        isHtml = true
                    )
                }
            }
        }

        override fun sendActivationEmail(account: User) = sendEmailFromTemplate(
            account.apply {
                d("Sending activation email to '${account.email}'")
            }, TEMPLATE_NAME_SIGNUP, TITLE_KEY_SIGNUP
        )

        override fun sendCreationEmail(account: User) = sendEmailFromTemplate(
            account.apply {
                d("Sending creation email to '${account.email}'")
            }, TEMPLATE_NAME_CREATION, TITLE_KEY_SIGNUP
        )

        override fun sendPasswordResetMail(account: User) = sendEmailFromTemplate(
            account.apply {
                d("Sending password reset email to '${account.email}'")
            }, TEMPLATE_NAME_PASSWORD, TITLE_KEY_PASSWORD
        )
    }
}