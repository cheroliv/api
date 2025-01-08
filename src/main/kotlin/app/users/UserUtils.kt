package app.users

import app.core.Constants
import app.core.Loggers
import app.core.database.EntityModel
import app.core.web.HttpUtils.badResponse
import app.core.web.HttpUtils.validator
import app.core.web.ProblemsModel
import app.users.signup.Signup
import app.users.signup.UserActivation
import app.users.signup.SignupController
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.status
import org.springframework.web.server.ServerWebExchange
import java.net.URI

object UserUtils {
    const val ONE_ROW_UPDATED = 1L

    @JvmStatic
    val SIGNUP_AVAILABLE = Triple(true, true, true)

    @JvmStatic
    val SIGNUP_LOGIN_NOT_AVAILABLE = Triple(false, true, false)

    @JvmStatic
    val SIGNUP_EMAIL_NOT_AVAILABLE = Triple(false, false, true)

    @JvmStatic
    val SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE = Triple(false, false, false)

    @JvmStatic
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
                        EntityModel.MODEL_FIELD_OBJECTNAME to Signup.objectName,
                        EntityModel.MODEL_FIELD_FIELD to first,
                        EntityModel.MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
    }


    @JvmStatic
    fun UserActivation.validate(
        exchange: ServerWebExchange
    ): Set<Map<String, String?>> = exchange.validator.run {
        "Validate UserActivation : ${this@validate}".run(Loggers::i)
        setOf(UserActivation.Attributes.ACTIVATION_KEY_ATTR)
            .map { it to validateProperty(this@validate, it) }
            .flatMap { (first, second) ->
                second.map {
                    mapOf<String, String?>(
                        EntityModel.MODEL_FIELD_OBJECTNAME to UserActivation.objectName,
                        EntityModel.MODEL_FIELD_FIELD to first,
                        EntityModel.MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
    }

    @JvmStatic
    val signupProblems: ProblemsModel =
        Constants.defaultProblems.copy(path = "${SignupController.UserRestApiRoutes.API_USERS}${SignupController.UserRestApiRoutes.API_SIGNUP}")

    @JvmStatic
    val activateProblems: ProblemsModel =
        Constants.defaultProblems.copy(path = "${SignupController.UserRestApiRoutes.API_USERS}${SignupController.UserRestApiRoutes.API_ACTIVATE}")

    @JvmStatic
    fun ProblemsModel.exceptionProblem(
        ex: Throwable,
        status: HttpStatus,
        obj: Class<*>
    ): ResponseEntity<ProblemDetail> =
        ProblemDetail.forStatus(status).apply {
            type = URI(activateProblems.type)
            setProperty("path", path)
            setProperty("message", message)
            setProperty(
                "fieldErrors", setOf(
                    mapOf(
                        EntityModel.MODEL_FIELD_OBJECTNAME to obj.simpleName.run {
                            replaceFirst(
                                first(),
                                first().lowercaseChar()
                            )
                        },
                        EntityModel.MODEL_FIELD_MESSAGE to ex.message
                    )
                )
            )
        }.run { status(status).body(this) }


    @JvmStatic
    val ProblemsModel.badResponseLoginAndEmailIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    EntityModel.MODEL_FIELD_OBJECTNAME to User.objectName,
                    EntityModel.MODEL_FIELD_FIELD to User.Fields.LOGIN_FIELD,
                    EntityModel.MODEL_FIELD_FIELD to User.Fields.EMAIL_FIELD,
                    EntityModel.MODEL_FIELD_MESSAGE to "Login name already used and email is already in use!!"
                )
            )
        )

    @JvmStatic
    val ProblemsModel.badResponseLoginIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    EntityModel.MODEL_FIELD_OBJECTNAME to Signup.objectName,
                    EntityModel.MODEL_FIELD_FIELD to User.Fields.LOGIN_FIELD,
                    EntityModel.MODEL_FIELD_MESSAGE to "Login name already used!"
                )
            )
        )

    @JvmStatic
    val ProblemsModel.badResponseEmailIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    EntityModel.MODEL_FIELD_OBJECTNAME to Signup.objectName,
                    EntityModel.MODEL_FIELD_FIELD to User.Fields.EMAIL_FIELD,
                    EntityModel.MODEL_FIELD_MESSAGE to "Email is already in use!"
                )
            )
        )
}