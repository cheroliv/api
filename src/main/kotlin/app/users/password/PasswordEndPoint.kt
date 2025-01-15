@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package app.users.password

import app.users.core.models.User.EndPoint.API_USER

object PasswordEndPoint {

    const val API_RESET_PASSWORD_INIT = "/reset-password/init"
    const val API_RESET_PASSWORD_INIT_PATH = "$API_USER$API_RESET_PASSWORD_INIT"

    const val API_RESET_PASSWORD_FINISH = "/reset-password/finish"
    const val API_RESET_PASSWORD_FINISH_PATH = "$API_USER$API_RESET_PASSWORD_FINISH"

    const val API_CHANGE_PASSWORD = "/change-password"
    const val API_CHANGE_PASSWORD_PATH = "$API_USER$API_CHANGE_PASSWORD"
}