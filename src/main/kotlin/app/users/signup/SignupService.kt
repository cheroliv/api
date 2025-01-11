package app.users.signup

import app.core.Loggers.i
import app.core.database.EntityModel.Members.withId
import app.core.web.HttpUtils.badResponse
import app.users.User
import app.users.signup.SignupEndPoint.activateProblems
import app.users.signup.SignupEndPoint.badResponseEmailIsNotAvailable
import app.users.signup.SignupEndPoint.badResponseLoginAndEmailIsNotAvailable
import app.users.signup.SignupEndPoint.badResponseLoginIsNotAvailable
import app.users.signup.SignupEndPoint.exceptionProblem
import app.users.UserDao.signup
import app.users.UserDao.signupAvailability
import app.users.signup.SignupEndPoint.signupProblems
import app.users.UserDao.signupToUser
import app.users.signup.SignupEndPoint.API_ACTIVATE_PATH
import app.users.signup.SignupEndPoint.API_ACTIVATE
import app.users.User.EndPoint.API_USERS
import app.users.signup.SignupEndPoint.validate
import app.users.signup.UserActivationDao.activateDao
import app.users.signup.UserActivationDao.validate
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import org.springframework.context.ApplicationContext
import org.springframework.core.env.get
import org.springframework.http.HttpStatus.*
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.nio.channels.AlreadyBoundException
import java.util.UUID.randomUUID

@Service
class SignupService(private val context: ApplicationContext) {
    companion object {
        const val ONE_ROW_UPDATED = 1L

        @JvmStatic
        val SIGNUP_AVAILABLE = Triple(true, true, true)

        @JvmStatic
        val SIGNUP_LOGIN_NOT_AVAILABLE = Triple(false, true, false)

        @JvmStatic
        val SIGNUP_EMAIL_NOT_AVAILABLE = Triple(false, false, true)

        @JvmStatic
        val SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE = Triple(false, false, false)
    }

    suspend fun signup(signup: Signup): Either<Throwable, User> = try {
        context.signupToUser(signup).run {
            (this to context).signup().mapLeft {
                return Exception("Unable to sign up user with this value : $signup", it).left()
            }.map {
                return apply {
                    i("Activation key: ${it.second}")
                    i("Activation link : http://localhost:${context.environment["server.port"]}/$API_ACTIVATE${it.second}")
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

    suspend fun activate(key: String): Long = context.activateDao(key)
        .getOrElse { throw IllegalStateException("Error activating user with key: $key", it) }
        .takeIf { it == ONE_ROW_UPDATED }
        ?: throw IllegalArgumentException("Activation failed: No user was activated for key: $key")

    suspend fun activate(
        key: String,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = UserActivation(
        id = randomUUID(),
        activationKey = key
    ).validate(exchange).run {
        "User activation attempt with key: $this".run(::i)
        if (isNotEmpty()) return activateProblems
            .copy(path = "$API_USERS$API_ACTIVATE_PATH")
            .badResponse(this)
    }.run {
        try {
            when (ONE_ROW_UPDATED) {
                activate(key) -> OK.run(::ResponseEntity)
                else -> signupProblems
                    .copy(path = "$API_USERS$API_ACTIVATE_PATH")
                    .exceptionProblem(
                        AlreadyBoundException(),
                        UNPROCESSABLE_ENTITY,
                        UserActivation::class.java
                    )
            }
        } catch (ise: IllegalStateException) {
            signupProblems
                .copy(path = "$API_USERS$API_ACTIVATE_PATH")
                .exceptionProblem(
                    ise,
                    EXPECTATION_FAILED,
                    UserActivation::class.java
                )
        } catch (iae: IllegalArgumentException) {
            signupProblems
                .copy(path = "$API_USERS$API_ACTIVATE_PATH")
                .exceptionProblem(
                    iae,
                    PRECONDITION_FAILED,
                    UserActivation::class.java
                )
        }
    }
}