package app.users.signup

import app.core.Constants
import app.core.database.EntityModel.Companion.MODEL_FIELD_FIELD
import app.core.database.EntityModel.Companion.MODEL_FIELD_MESSAGE
import app.core.database.EntityModel.Companion.MODEL_FIELD_OBJECTNAME
import app.core.web.HttpUtils.badResponse
import app.core.web.HttpUtils.validator
import app.core.web.ProblemsModel
import app.users.User
import app.users.User.EndPoint.API_USERS
import app.users.User.Relations.Fields.EMAIL_FIELD
import app.users.User.Relations.Fields.LOGIN_FIELD
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.status
import org.springframework.web.server.ServerWebExchange
import java.net.URI

/** SignupEndPoint REST API URIs */
object SignupEndPoint {
    const val API_SIGNUP = "/signup"
    const val API_SIGNUP_PATH = "$API_USERS$API_SIGNUP"

    const val API_ACTIVATE = "/activate"
    const val API_ACTIVATE_KEY = "key"
    const val API_ACTIVATE_PARAM = "{activationKey}"
    const val API_ACTIVATE_PATH = "$API_USERS$API_ACTIVATE?$API_ACTIVATE_KEY="

    @JvmStatic
    val signupProblems: ProblemsModel =
        Constants.defaultProblems.copy(path = "$API_USERS$API_SIGNUP")

    @JvmStatic
    val activateProblems: ProblemsModel =
        Constants.defaultProblems.copy(path = "$API_USERS$API_ACTIVATE")

    @JvmStatic
    fun ProblemsModel.exceptionProblem(
        ex: Throwable,
        status: HttpStatus,
        obj: Class<*>
    ): ResponseEntity<ProblemDetail> =
        forStatus(status).apply {
            type = URI(activateProblems.type)
            setProperty("path", path)
            setProperty("message", message)
            setProperty(
                "fieldErrors", setOf(
                    mapOf(
                        MODEL_FIELD_OBJECTNAME to obj.simpleName.run {
                            replaceFirst(
                                first(),
                                first().lowercaseChar()
                            )
                        },
                        MODEL_FIELD_MESSAGE to ex.message
                    )
                )
            )
        }.run { status(status).body(this) }


    @JvmStatic
    val ProblemsModel.badResponseLoginAndEmailIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    MODEL_FIELD_OBJECTNAME to User.objectName,
                    MODEL_FIELD_FIELD to LOGIN_FIELD,
                    MODEL_FIELD_FIELD to EMAIL_FIELD,
                    MODEL_FIELD_MESSAGE to "Login name already used and email is already in use!!"
                )
            )
        )

    @JvmStatic
    val ProblemsModel.badResponseLoginIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    MODEL_FIELD_OBJECTNAME to Signup.objectName,
                    MODEL_FIELD_FIELD to LOGIN_FIELD,
                    MODEL_FIELD_MESSAGE to "Login name already used!"
                )
            )
        )

    @JvmStatic
    val ProblemsModel.badResponseEmailIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    MODEL_FIELD_OBJECTNAME to Signup.objectName,
                    MODEL_FIELD_FIELD to EMAIL_FIELD,
                    MODEL_FIELD_MESSAGE to "Email is already in use!"
                )
            )
        )

    fun Signup.validate(
        exchange: ServerWebExchange
    ): Set<Map<String, String?>> = exchange.validator.run {
        setOf(
            User.Attributes.PASSWORD_ATTR,
            User.Attributes.EMAIL_ATTR,
            User.Attributes.LOGIN_ATTR,
        ).map { it to validateProperty(this@validate, it) }
            .flatMap { (first, second) ->
                second.map {
                    mapOf<String, String?>(
                        MODEL_FIELD_OBJECTNAME to Signup.objectName,
                        MODEL_FIELD_FIELD to first,
                        MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
    }
}