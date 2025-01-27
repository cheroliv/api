package app.users.password

import app.users.core.Loggers.d
import app.users.core.dao.UserDao.change
import app.users.core.dao.UserDao.findOne
import app.users.core.models.User
import app.users.core.models.User.Attributes.EMAIL_ATTR
import app.users.core.models.User.Relations.UPDATE_PASSWORD_RESET
import app.users.core.security.SecurityUtils.generateResetKey
import app.users.core.security.SecurityUtils.getCurrentUserLogin
import app.users.core.web.HttpUtils.validator
import app.users.mail.UserMailService
import app.users.password.PasswordChange.Attributes.NEW_PASSWORD_ATTR
import app.users.password.UserReset.Attributes.RESET_KEY_ATTR
import app.users.password.UserReset.Relations.FIND_BY_KEY
import app.users.password.UserReset.Relations.UPDATE_CHANGE_DATE_IS_ACTIVE
import app.users.signup.SignupService.Companion.ONE_ROW_UPDATED
import app.users.signup.SignupService.Companion.TWO_ROWS_UPDATED
import app.users.signup.SignupService.Companion.ZERO_ROW_UPDATED
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.of
import org.springframework.http.ResponseEntity.ok
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.validation.annotation.Validated
import org.springframework.web.server.ServerWebExchange
import java.util.UUID

@Service
@Validated
class PasswordService(val context: ApplicationContext) {
    suspend fun reset(
        @Email mail: String, exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = try {
        reset(mail).run {
            when (isRight()) {
                true -> ok()

                else -> of(
                    forStatusAndDetail(
                        INTERNAL_SERVER_ERROR,
                        swap().getOrNull()?.message
                    )
                )
            }
        }
    } catch (t: Throwable) {
        of(forStatusAndDetail(BAD_REQUEST, t.message))
    }.build()

    /**
     * @return {@code Either<Throwable, String>} where String is the reset key.
     */
    suspend fun reset(@Email mail: String): Either<Throwable, String> = try {
        generateResetKey.let { resetKey ->
            ((mail to resetKey) to context).reset().map { updatedRows ->
                when (updatedRows) {
                    ONE_ROW_UPDATED -> return resetKey
                        .apply {
                            context.findOne<User>(mail)
                                .map { user -> (user to resetKey).run(context.getBean<UserMailService>()::sendPasswordResetMail) }
                        }
                        .right()

                    ZERO_ROW_UPDATED -> throw IllegalStateException("user_reset not saved")
                    else -> throw IllegalStateException("not expected to save more than one user_reset")
                }
            }
        }
    } catch (t: Throwable) {
        t.left()
    }

    /**
     * @param Pair of mailKeyPair mail and key as first and ApplicationContext as second.
     */
    suspend fun Pair<Pair<String, String>, ApplicationContext>.reset()
            : Either<Throwable, Long> = try {
        UserReset.Relations.INSERT.trimIndent()
            .run(second.getBean<DatabaseClient>()::sql)
            .bind(EMAIL_ATTR, first.first)
            .bind(RESET_KEY_ATTR, first.second)
            .fetch()
            .awaitRowsUpdated()
            .right()
    } catch (e: Throwable) {
        Throwable(message = "Email not found", cause = e.cause).left()
    }

    /**
     * {@code 200 (Ok) POST   /user/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param reset            the generated key and the new password.
     * @throws InvalidPasswordProblem   {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Application Error)} if the password could not be reset.
     */
    suspend fun finish(
        reset: ResetPassword, exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = exchange.validator.validate(reset).run {
        when {
            isNotEmpty() -> of(
                forStatusAndDetail(BAD_REQUEST, iterator().next().message)
            )

            else -> try {
                when (finish(reset.newPassword, reset.key)) {
                    TWO_ROWS_UPDATED -> ok()

                    else -> of(
                        forStatusAndDetail(
                            INTERNAL_SERVER_ERROR,
                            "No user was found for this reset key"
                        )
                    )
                }
            } catch (t: Throwable) {
                when {
                    t.message?.contains("No user was found for this reset key") == true -> of(
                        forStatusAndDetail(
                            INTERNAL_SERVER_ERROR,
                            t.message
                        )
                    )

                    else -> of(
                        forStatusAndDetail(
                            BAD_REQUEST,
                            t.message
                        )
                    )
                }
            }
        }
    }.build()

    suspend fun finish(newPassword: String, key: String): Long = try {
        context.getBean<TransactionalOperator>().executeAndAwait {
            val database = context.getBean<DatabaseClient>()
            var res = 0L
            val encryptedNewPassword = newPassword
                .run(context.getBean<PasswordEncoder>()::encode)
            val userId: UUID = FIND_BY_KEY.trimMargin().run(database::sql)
                .bind("resetKey", key)
                .fetch().awaitSingleOrNull()?.get("user_id")
                .toString().run(UUID::fromString)
            //mise a jour du user_reset
            res += UPDATE_CHANGE_DATE_IS_ACTIVE.trimIndent()
                .run(context.getBean<DatabaseClient>()::sql)
                .bind("resetKey", key)
                .fetch()
                .awaitRowsUpdated()
            //mise a jour du user UPDATE_PASSWORD
            res += UPDATE_PASSWORD_RESET.trimIndent()
                .run(context.getBean<DatabaseClient>()::sql)
                .bind("password", encryptedNewPassword)
                .bind("id", userId)
                .fetch()
                .awaitRowsUpdated()
            res
        }
    } catch (t: Throwable) {
        throw Exception("No user was found for this reset key", t.cause)
    }


    suspend fun change(@Valid changePassword: PasswordChange): Long {
        getCurrentUserLogin().apply {
            d("Current security context user.login : $this")
            when {
                isNotBlank() -> {
                    context.findOne<User>(this).map {
                        when {
                            context.getBean<PasswordEncoder>().matches(
                                changePassword.currentPassword,
                                it.password
                            ) -> return (it.copy(password = changePassword.newPassword) to context).change()
                                //invalid update password persistence
                                .getOrElse { throw InvalidPasswordException() }
                                .apply { d("Changed password for User: ${it.login}") }
                            //invalid security context login
                            else -> throw InvalidPasswordException()
                        }
                    }
                }
            }
        }
        //invalid security authorization
        throw InvalidPasswordException()
    }

    suspend fun change(
        passwordChange: PasswordChange,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = exchange.validator.validateProperty(
        passwordChange,
        NEW_PASSWORD_ATTR
    ).run {
        when {
            isNotEmpty() -> of(
                forStatusAndDetail(BAD_REQUEST, iterator().next().message)
            )

            else -> try {
                change(passwordChange)
                ok()
            } catch (t: Throwable) {
                of(forStatusAndDetail(BAD_REQUEST, t.message))
            }
        }
    }.build()
}