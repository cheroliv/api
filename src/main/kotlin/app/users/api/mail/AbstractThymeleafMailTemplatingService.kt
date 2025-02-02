package app.users.api.mail

import app.users.api.Constants.BASE_URL
import app.users.api.Constants.TEMPLATE_NAME_CREATION
import app.users.api.Constants.TEMPLATE_NAME_PASSWORD
import app.users.api.Constants.TEMPLATE_NAME_SIGNUP
import app.users.api.Constants.TITLE_KEY_PASSWORD
import app.users.api.Constants.TITLE_KEY_SIGNUP
import app.users.api.Constants.USER
import app.users.api.Loggers.d
import app.users.api.Properties
import app.users.api.models.User
import app.users.password.UserReset.Attributes.RESET_KEY_ATTR
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import org.springframework.context.MessageSource
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.util.Locale.forLanguageTag

abstract class AbstractThymeleafMailTemplatingService(
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
        @Suppress("SENSELESS_NULL_IN_WHEN")
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
                        setVariable(RESET_KEY_ATTR, map[RESET_KEY_ATTR].toString())
                    }),
                    isMultipart = false,
                    isHtml = true
                )
            }
        }
    }



    override fun sendCreationEmail(userResetKeyPair: Pair<User, String>) = sendEmailFromTemplate(
        mapOf(User.objectName to userResetKeyPair.apply {
//            d("Sending creation email to '${first.email}'")
        }.first, RESET_KEY_ATTR to userResetKeyPair.second),
        TEMPLATE_NAME_CREATION,
        TITLE_KEY_PASSWORD
    )

    override fun sendActivationEmail(pairUserActivationKey: Pair<User, String>) =
        sendEmailFromTemplate(
            map = mapOf(User.objectName to pairUserActivationKey.apply {
//                d("Sending activation email to ${first.email}")
            }.first, ACTIVATION_KEY_ATTR to pairUserActivationKey.second),
            templateName = TEMPLATE_NAME_SIGNUP,
            titleKey = TITLE_KEY_SIGNUP
        )

    override fun sendPasswordResetMail(userResetKeyPair: Pair<User, String>) =
        sendEmailFromTemplate(
            map = mapOf(User.objectName to userResetKeyPair.apply {
//                d("Sending password reset email to '${first.email}'")
            }.first, RESET_KEY_ATTR to userResetKeyPair.second),
            templateName = TEMPLATE_NAME_PASSWORD,
            titleKey = TITLE_KEY_PASSWORD
        )
}