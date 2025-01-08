package app.users

import app.core.Loggers.i
import app.core.database.EntityModel.Members.withId
import app.core.web.HttpUtils.badResponse
import app.users.UserController.UserRestApiRoutes.API_ACTIVATE
import app.users.UserController.UserRestApiRoutes.API_ACTIVATE_PATH
import app.users.UserController.UserRestApiRoutes.API_USERS
import app.users.UserDao.signupAvailability
import app.users.UserDao.signup
import app.users.UserDao.signupToUser
import app.users.UserUtils.ONE_ROW_UPDATED
import app.users.UserUtils.SIGNUP_EMAIL_NOT_AVAILABLE
import app.users.UserUtils.SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE
import app.users.UserUtils.SIGNUP_LOGIN_NOT_AVAILABLE
import app.users.UserUtils.activateProblems
import app.users.UserUtils.badResponseEmailIsNotAvailable
import app.users.UserUtils.badResponseLoginAndEmailIsNotAvailable
import app.users.UserUtils.badResponseLoginIsNotAvailable
import app.users.UserUtils.exceptionProblem
import app.users.UserUtils.signupProblems
import app.users.UserUtils.validate
import app.users.signup.Signup
import app.users.signup.UserActivation
import app.users.signup.UserActivationDao.activateDao
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus.*
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.nio.channels.AlreadyBoundException
import java.util.UUID.randomUUID

@Service
class UserService(private val context: ApplicationContext) {
    suspend fun signup(signup: Signup): Either<Throwable, User> = try {
        context.signupToUser(signup).run {
            (this to context).signup().mapLeft {
                return Exception("Unable to sign up user with this value : $signup", it).left()
            }.map {
                return apply {
                    i("Activation key: ${it.second}")
                    i("Activation link : http://localhost$API_ACTIVATE_PATH${it.second}")
                }.withId(it.first).right()
            }
        }
    } catch (t: Throwable) {
        t.left()
    }

    suspend fun signupAvailability(signup: Signup)
            : Either<Throwable, Triple<Boolean, Boolean, Boolean>> = try {
        (signup to context)
            .signupAvailability()
            .onRight { it.right() }
            .onLeft { it.left() }
    } catch (ex: Throwable) {
        ex.left()
    }

    suspend fun signup(signup: Signup, exchange: ServerWebExchange)
            : ResponseEntity<ProblemDetail> = signup
        .validate(exchange)
        .run {
            "signup attempt: ${this@run} ${signup.login} ${signup.email}".run(::i)
            if (isNotEmpty()) return signupProblems.badResponse(this)
        }.run {
            signupAvailability(signup).map {
                return when (it) {
                    SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE -> signupProblems.badResponseLoginAndEmailIsNotAvailable
                    SIGNUP_LOGIN_NOT_AVAILABLE -> signupProblems.badResponseLoginIsNotAvailable
                    SIGNUP_EMAIL_NOT_AVAILABLE -> signupProblems.badResponseEmailIsNotAvailable
                    else -> {
                        signup(signup).run { CREATED.run(::ResponseEntity) }
                    }
                }
            }
            SERVICE_UNAVAILABLE.run(::ResponseEntity)
        }

    suspend fun activate(
        key: String,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = UserActivation(
        id = randomUUID(),
        activationKey = key
    ).validate(exchange).run {
        "User activation attempt with key: $this".run(::i)
        if (isNotEmpty()) return activateProblems
            .copy(path = "$API_USERS$API_ACTIVATE")
            .badResponse(this)
    }.run {
        try {
            when (ONE_ROW_UPDATED) {
                activate(key) -> OK.run(::ResponseEntity)
                else -> signupProblems
                    .copy(path = "$API_USERS$API_ACTIVATE")
                    .exceptionProblem(
                        AlreadyBoundException(),
                        UNPROCESSABLE_ENTITY,
                        UserActivation::class.java
                    )
            }
        } catch (ise: IllegalStateException) {
            signupProblems
                .copy(path = "$API_USERS$API_ACTIVATE")
                .exceptionProblem(
                    ise,
                    EXPECTATION_FAILED,
                    UserActivation::class.java
                )
        } catch (iae: IllegalArgumentException) {
            signupProblems
                .copy(path = "$API_USERS$API_ACTIVATE")
                .exceptionProblem(
                    iae,
                    PRECONDITION_FAILED,
                    UserActivation::class.java
                )
        }
    }

    suspend fun activate(key: String): Long = context.activateDao(key)
        .getOrElse { throw IllegalStateException("Error activating user with key: $key", it) }
        .takeIf { it == ONE_ROW_UPDATED }
        ?: throw IllegalArgumentException("Activation failed: No user was activated for key: $key")

}