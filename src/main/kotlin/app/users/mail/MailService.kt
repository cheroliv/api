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
        user: User,
        templateName: String,
        titleKey: String
    )

    fun sendPasswordResetMail(user: User)
    fun sendActivationEmail(pairUserActivationKey: Pair<User, String>)
    fun sendCreationEmail(user: User)

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
            user: User,
            templateName: String,
            titleKey: String
        ) {
            @Suppress(
                "SENSELESS_NULL_IN_WHEN"
            )
            when (user.email) {
                null -> {
                    d("Email doesn't exist for user '${user.login}'")
                    return
                }

                else -> forLanguageTag(user.langKey).apply {
                    sendEmail(
                        user.email,
                        messageSource.getMessage(titleKey, null, this),
                        templateEngine.process(templateName, Context(this).apply {
                            setVariable(USER, user)
                            setVariable(BASE_URL, properties.mail.baseUrl)
                        }),
                        isMultipart = false,
                        isHtml = true
                    )
                }
            }
        }

        override fun sendActivationEmail(pairUserActivationKey: Pair<User, String>) = sendEmailFromTemplate(
            pairUserActivationKey.first.apply {
                d("Sending activation email to $email")
            }, TEMPLATE_NAME_SIGNUP, TITLE_KEY_SIGNUP
        )

        override fun sendCreationEmail(user: User) = sendEmailFromTemplate(
            user.apply {
                d("Sending creation email to '${user.email}'")
            }, TEMPLATE_NAME_CREATION, TITLE_KEY_SIGNUP
        )

        override fun sendPasswordResetMail(user: User) = sendEmailFromTemplate(
            user.apply {
                d("Sending password reset email to '${user.email}'")
            }, TEMPLATE_NAME_PASSWORD, TITLE_KEY_PASSWORD
        )
    }
}