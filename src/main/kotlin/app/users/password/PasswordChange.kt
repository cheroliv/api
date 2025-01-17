package app.users.password

import app.users.signup.Signup.Constraints.PASSWORD_MAX
import app.users.signup.Signup.Constraints.PASSWORD_MIN
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size


data class PasswordChange(
    @field:NotNull
    @field:Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
    val currentPassword: String,
    @field:NotNull
    @field:Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
    val newPassword: String
) {
    object Attributes {
        const val NEW_PASSWORD_ATTR = "newPassword"
        const val CURRENT_PASSWORD_ATTR = "currentPassword"
    }
}