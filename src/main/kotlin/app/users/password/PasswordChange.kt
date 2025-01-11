package app.users.password

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@JvmRecord
data class PasswordChange(
    @field:NotNull
    @field:Size(min = 4, max = 20)
    val currentPassword: String,
    @field:NotNull
    @field:Size(min = 4, max = 20)
    val newPassword: String
) {
    object Attributes {
        const val NEW_PASSWORD_ATTR = "newPassword"
        const val CURRENT_PASSWORD_ATTR = "currentPassword"
    }
}