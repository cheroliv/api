package app.users.signup

import app.users.core.models.User.Constraints.LOGIN_REGEX
import app.users.core.models.User.EndPoint.API_USER
import app.users.core.models.User.EndPoint.API_USERS
import app.users.signup.Signup.Constraints.PASSWORD_MAX
import app.users.signup.Signup.Constraints.PASSWORD_MIN
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size


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
    @field:Size(min = 7, max = 254)
    val email: String,
) {

    object Constraints {
        const val PASSWORD_MIN: Int = 4
        const val PASSWORD_MAX: Int = 20
    }

    /** SignupEndPoint REST API URIs */
    object EndPoint {
        const val API_SIGNUP = "/signup"
        const val API_SIGNUP_PATH = "$API_USER$API_SIGNUP"

        const val API_ACTIVATE = "/activate"
        const val API_ACTIVATE_KEY = "key"
        const val API_ACTIVATE_PARAM = "{activationKey}"
        const val API_ACTIVATE_PATH = "$API_USER$API_ACTIVATE?$API_ACTIVATE_KEY="
    }

    companion object {
        @JvmStatic
        val objectName: String = Signup::class
            .java
            .simpleName
            .run { replaceFirst(first(), first().lowercaseChar()) }
    }
}