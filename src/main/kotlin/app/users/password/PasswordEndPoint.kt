@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package app.users.password

import app.users.User.EndPoint.API_USERS

object PasswordEndPoint {
    const val API_RESET_PASSWORD_INIT_PATH = "/reset-password/init"
    const val API_RESET_PASSWORD_INIT = "$API_USERS$API_RESET_PASSWORD_INIT_PATH"

    const val API_RESET_PASSWORD_FINISH_PATH = "/reset-password/finish"
    const val API_RESET_PASSWORD_FINISH = "$API_USERS$API_RESET_PASSWORD_FINISH_PATH"

    const val API_CHANGE_PASSWORD_PATH = "/change-password"
    const val API_CHANGE_PASSWORD = "$API_USERS$API_CHANGE_PASSWORD_PATH"
}