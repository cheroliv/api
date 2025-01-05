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
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import org.springframework.context.MessageSource
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.util.Locale.forLanguageTag

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
        map: Map<String, Any>,
        templateName: String,
        titleKey: String
    ) {
        @Suppress(
            "SENSELESS_NULL_IN_WHEN"
        )
        when ((map[User.objectName] as User).email) {
            null -> {
                d("Email doesn't exist for user '${(map[User.objectName] as User).login}'")
                return
            }

            else -> forLanguageTag((map[User.objectName] as User).langKey).apply {
                sendEmail(
                    (map[User.objectName] as User).email,
                    messageSource.getMessage(titleKey, null, this),
                    templateEngine.process(templateName, Context(this).apply {
                        setVariable(USER, map[User.objectName])
                        setVariable(ACTIVATION_KEY_ATTR, map[ACTIVATION_KEY_ATTR])
                        setVariable(BASE_URL, properties.mail.baseUrl)
                        setVariable("resetKey", map["resetKey"].toString())
                    }),
                    isMultipart = false,
                    isHtml = true
                )
            }
        }
    }

    override fun sendActivationEmail(pairUserActivationKey: Pair<User, String>) = sendEmailFromTemplate(
        mapOf(User.objectName to pairUserActivationKey.first.apply {
            d("Sending activation email to $email whith key ${pairUserActivationKey.second}")
        }), TEMPLATE_NAME_SIGNUP, TITLE_KEY_SIGNUP
    )

    override fun sendCreationEmail(userResetKeyPair: Pair<User, String>) = sendEmailFromTemplate(
        mapOf(User.objectName to userResetKeyPair.apply {
            d("Sending creation email to '${first.email}' with reset key : $second")
        }.first, "resetKey" to userResetKeyPair.second), TEMPLATE_NAME_CREATION, TITLE_KEY_PASSWORD
    )

    override fun sendPasswordResetMail(user: User) = sendEmailFromTemplate(
        mapOf(User.objectName to user.apply {
            d("Sending password reset email to '${user.email}'")
        }), TEMPLATE_NAME_PASSWORD, TITLE_KEY_PASSWORD
    )
}