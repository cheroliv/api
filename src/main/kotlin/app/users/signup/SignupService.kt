package app.users.signup

import app.users.core.Loggers.i
import app.users.core.dao.UserDao.availability
import app.users.core.dao.UserDao.signup
import app.users.core.dao.UserDao.user
import app.users.core.models.EntityModel.Members.withId
import app.users.core.models.User
import app.users.core.models.User.EndPoint.API_USERS
import app.users.core.web.HttpUtils.badResponse
import app.users.signup.Signup.EndPoint.API_ACTIVATE
import app.users.signup.Signup.EndPoint.API_ACTIVATE_PATH
import app.users.signup.SignupDao.activate
import app.users.signup.SignupDao.validate
import app.users.signup.SignupErrors.activateProblems
import app.users.signup.SignupErrors.badResponseEmailIsNotAvailable
import app.users.signup.SignupErrors.badResponseLoginAndEmailIsNotAvailable
import app.users.signup.SignupErrors.badResponseLoginIsNotAvailable
import app.users.signup.SignupErrors.exceptionProblem
import app.users.signup.SignupErrors.signupProblems
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import org.springframework.context.ApplicationContext
import org.springframework.core.env.get
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.EXPECTATION_FAILED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.nio.channels.AlreadyBoundException
import java.util.UUID.randomUUID

@Service
class SignupService(private val context: ApplicationContext) {

    suspend fun signup(signup: Signup): Either<Throwable, User> = try {
        context.user(signup).run {
            (this to context).signup().mapLeft {
                return Exception("Unable to sign up user with this value : $signup", it).left()
            }.map {
                return apply {
                    i("Activation key: ${it.second}")
                    i("Activation link : http://localhost:${context.environment["server.port"]}/$API_ACTIVATE_PATH${it.second}")
                }.withId(it.first).right()
            }
        }
    } catch (t: Throwable) {
        t.left()
    }

    suspend fun availability(signup: Signup)
            : Either<Throwable, Triple<Boolean, Boolean, Boolean>> = try {
        (signup to context)
            .availability()
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
            availability(signup).map {
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

    suspend fun activate(key: String): Long = context.activate(key)
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
}