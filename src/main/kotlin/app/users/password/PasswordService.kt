package app.users.password

import app.users.core.Constants.ENCRYPTER_BEAN_NAME
import app.users.core.Loggers.d
import app.users.core.Loggers.i
import app.users.core.dao.UserDao.change
import app.users.core.dao.UserDao.findOne
import app.users.core.models.User
import app.users.core.models.User.Attributes.EMAIL_ATTR
import app.users.core.models.User.Attributes.PASSWORD_ATTR
import app.users.core.security.SecurityUtils.generateResetKey
import app.users.core.security.SecurityUtils.getCurrentUserLogin
import app.users.core.web.HttpUtils.validator
import app.users.password.PasswordChange.Attributes.NEW_PASSWORD_ATTR
import app.users.password.UserReset.Attributes.RESET_KEY_ATTR
import app.users.signup.SignupService.Companion.ONE_ROW_UPDATED
import app.users.signup.SignupService.Companion.TWO_ROW_UPDATED
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
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.security.crypto.encrypt.TextEncryptor
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
    private suspend fun mail(mailKeyPair: Pair<String, String>) {
        i("generated key for reset password : ${mailKeyPair.second}\n\tTODO: link to finish reset password")
//        i("generated key for reset password : ${mailKeyPair.second}")
//        return userRepository
//            .findOneByEmail(mail)
//            .apply {
//                if (this != null && this.activated) {
//                    resetKey = generateResetKey
//                    resetDate = now()
//                    saveUser(this)
//                } else return null
//            }
    }

    suspend fun reset(
        @Email mail: String,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> =
        try {
            reset(mail).run {
                when (isRight()) {
                    true -> map { mail(mailKeyPair = mail to it) }.run { ok() }

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
        generateResetKey.run {
            ((mail to this) to context).reset().map {
                when (it) {
                    ONE_ROW_UPDATED -> return right()
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
        context.getBean<TextEncryptor>(ENCRYPTER_BEAN_NAME).run {
            UserReset.Relations.INSERT.trimIndent()
                .run(second.getBean<DatabaseClient>()::sql)
                .bind(EMAIL_ATTR, first.first)
                .bind(RESET_KEY_ATTR, first.second.run(::encrypt))
                .fetch()
                .awaitRowsUpdated()
                .right()
        }
    } catch (e: Throwable) {
        Throwable(message = "Email not found", cause = e.cause).left()
    }

    /**
     * {@code 200 (Ok) POST   /user/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Application Error)} if the password could not be reset.
     */
    suspend fun finish(
        @Valid keyAndPassword: KeyAndPassword,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = try {
        when (TWO_ROW_UPDATED) {
            finish(
                keyAndPassword.newPassword!!,
                keyAndPassword.key!!
            ).apply { "Row updated: $this".run(::i) } -> ok()

            else -> of(
                forStatusAndDetail(
                    INTERNAL_SERVER_ERROR,
                    "No user was found for this reset key"
                )
            )
        }
    } catch (t: Throwable) {
        of(forStatusAndDetail(BAD_REQUEST, t.message))
    }.build()


    suspend fun finish(newPassword: String, key: String): Long {
        var res: Long = 0L
        val encryptedKey = key.run(context.getBean<TextEncryptor>(ENCRYPTER_BEAN_NAME)::encrypt)
        val userId = """
        SELECT ur."user_id" FROM "user_reset" ur 
        WHERE ur."reset_key" = :resetKey 
        AND ur."is_active" = TRUE;
        """.trimIndent()
            .run(context.getBean<DatabaseClient>()::sql)
            .bind(RESET_KEY_ATTR, encryptedKey)
            .fetch()
            .awaitOne()["user_id"].toString()
            .run(UUID::fromString)

        context.getBean<TransactionalOperator>().executeAndAwait {
            //mise a jour du user_reset
            res += """
        UPDATE user_reset 
        SET is_active = FALSE, 
        change_date = NOW()
        WHERE reset_key = :resetKey                
        """.trimIndent()
                .run(context.getBean<DatabaseClient>()::sql)
                .bind(RESET_KEY_ATTR, encryptedKey)
                .fetch()
                .awaitRowsUpdated()

            context.getBean<TransactionalOperator>().executeAndAwait {
                //mise a jour du user
                res += """
                UPDATE "user"
                SET "password" = :password, "version" = version + 1
                WHERE "id" = :id
                """.trimIndent()
                    .run(context.getBean<DatabaseClient>()::sql)
                    .bind(PASSWORD_ATTR, context.getBean<PasswordEncoder>().encode(newPassword))
                    .bind(User.Attributes.ID_ATTR, userId)
                    .fetch()
                    .awaitRowsUpdated()
            }
        }
        return res
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