package app.users.mail

import app.users.User


interface MailService {
    fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    )

    fun sendEmailFromTemplate(
        map: Map<String, Any>,
        templateName: String,
        titleKey: String
    )

    fun sendPasswordResetMail(user: User)
    fun sendActivationEmail(pairUserActivationKey: Pair<User, String>)
    fun sendCreationEmail(user: User)

}