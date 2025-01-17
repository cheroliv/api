package app.users.signup

import app.users.core.Constants
import app.users.core.models.EntityModel.Companion.MODEL_FIELD_FIELD
import app.users.core.models.EntityModel.Companion.MODEL_FIELD_MESSAGE
import app.users.core.models.EntityModel.Companion.MODEL_FIELD_OBJECTNAME
import app.users.core.models.User
import app.users.core.models.User.EndPoint.API_USERS
import app.users.core.models.User.Relations.Fields.EMAIL_FIELD
import app.users.core.models.User.Relations.Fields.LOGIN_FIELD
import app.users.core.web.HttpUtils.badResponse
import app.users.core.web.ProblemsModel
import app.users.signup.Signup.EndPoint.API_ACTIVATE
import app.users.signup.Signup.EndPoint.API_SIGNUP
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.status
import java.net.URI

object SignupErrors {
    
    val signupProblems: ProblemsModel =
        Constants.defaultProblems.copy(path = "$API_USERS$API_SIGNUP")

    
    val activateProblems: ProblemsModel =
        Constants.defaultProblems.copy(path = "$API_USERS$API_ACTIVATE")

    
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


    
    val ProblemsModel.badResponseLoginAndEmailIsNotAvailable: ResponseEntity<ProblemDetail>
        get() = badResponse(
            setOf(
                mapOf(
                    MODEL_FIELD_OBJECTNAME to User.objectName,
                    MODEL_FIELD_FIELD to LOGIN_FIELD,
                    MODEL_FIELD_FIELD to EMAIL_FIELD,
                    MODEL_FIELD_MESSAGE to "Login name already used and email is already in use!"
                )
            )
        )

    
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


}