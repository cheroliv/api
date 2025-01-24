package app.users.password

import app.users.signup.Signup.Constraints.PASSWORD_MAX
import app.users.signup.Signup.Constraints.PASSWORD_MIN
import jakarta.validation.constraints.Size


data class ResetPassword(
    @field:Size(max = 20)
    val key: String,
    @field:Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
    val newPassword: String
)