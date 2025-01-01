package app.users.signup

import app.Constants.PASSWORD_MAX
import app.Constants.PASSWORD_MIN
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import app.users.User.Companion.LOGIN_REGEX


@JvmRecord
@FieldMatch(
    first = "password",
    second = "repassword",
    message = "The password fields must match"
)
data class Signup(
    @field:NotNull
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    val login: String,
    @field:NotNull
    @field:Size(
        min = PASSWORD_MIN,
        max = PASSWORD_MAX
    )
    val password: String,
    val repassword: String,
    @field:Email
    @field:Size(min = 5, max = 254)
    val email: String,
) {
    companion object {
        @JvmStatic
        val objectName: String = Signup::class
            .java
            .simpleName.run {
                replaceFirst(
                    first(),
                    first().lowercaseChar()
                )
            }
    }
}