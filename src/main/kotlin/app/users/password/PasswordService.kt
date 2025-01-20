package app.users.password

import app.users.core.Loggers.d
import app.users.core.Loggers.i
import app.users.core.dao.UserDao.change
import app.users.core.dao.UserDao.findOne
import app.users.core.models.User
import app.users.core.models.User.Attributes.EMAIL_ATTR
import app.users.core.security.SecurityUtils.generateResetKey
import app.users.core.security.SecurityUtils.getCurrentUserLogin
import app.users.core.web.HttpUtils.validator
import app.users.password.PasswordChange.Attributes.NEW_PASSWORD_ATTR
import app.users.password.UserReset.Attributes.RESET_KEY_ATTR
import app.users.signup.SignupService.Companion.ONE_ROW_UPDATED
import app.users.signup.SignupService.Companion.ZERO_ROW_UPDATED
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.of
import org.springframework.http.ResponseEntity.ok
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.server.ServerWebExchange

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
            requestPasswordReset(mail).run {
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

    suspend fun requestPasswordReset(@Email mail: String): Either<Throwable, /*Reset key*/String> =
        try {
            generateResetKey.run {
                ((mail to this) to context).reset().map {
                    when (it) {
                        ONE_ROW_UPDATED -> return right()
                        ZERO_ROW_UPDATED -> throw IllegalStateException("user_reset not saved")
                        else -> throw IllegalStateException("not expected to save more than one user_reset")
                    }
                }
            }
        } catch (e: Exception) {
            e.left()
        }

    private suspend fun Pair<Pair</*mail*/String, /*key*/String>, ApplicationContext>.reset()
            : Either<Throwable, Long> = try {
        UserReset.Relations.INSERT.trimIndent()
            .run(second.getBean<R2dbcEntityTemplate>().databaseClient::sql)
            .bind(EMAIL_ATTR, first.first)
            .bind(RESET_KEY_ATTR, first.second.run(context.getBean<PasswordEncoder>()::encode))
            .fetch()
            .awaitRowsUpdated()
            .right()
    } catch (e: Throwable) {
        Throwable(message = "Email not found", cause = e.cause).left()
    }


    //TODO: find user id by key and update at the same time using the posted pair keyPassword
    suspend fun completePasswordReset(newPassword: String, key: String): User? {
        //        accountRepository.findOneByResetKey(key).run {
//            if (this != null && resetDate?.isAfter(Instant.now().minusSeconds(86400)) == true) {
//                d("Reset account password for reset key $key")
//                return@completePasswordReset toCredentialsModel
//                //                return saveUser(
//                //                apply {
//                ////                    password = passwordEncoder.encode(newPassword)
//                //                    resetKey = null
//                //                    resetDate = null
//                //                })
//            } else {
//                d("$key is not a valid reset account password key")
//                return@completePasswordReset null
//            }
        return null
    }


    /**
     * {@code POST   /user/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Application Error)} if the password could not be reset.
     */
    suspend fun finish(
        keyAndPassword: KeyAndPassword,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> {
        InvalidPasswordException().run {
//        when {
//            validator
//                .validateProperty(
//                    AccountCredentials(password = keyAndPassword.newPassword),
//                    PASSWORD_FIELD
//                ).isNotEmpty() -> throw this
//
//            keyAndPassword.newPassword != null
//                    && keyAndPassword.key != null
//                    && passwordService.completePasswordReset(
//                keyAndPassword.newPassword,
//                keyAndPassword.key
//            ) == null -> throw PasswordException("No user was found for this reset key")
        }
        return ok().build()
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


/*
package community.accounts.password

import community.API_ACCOUNT
import community.API_RESET_INIT
import community.accounts.Account
import community.accounts.Account.Companion.EMAIL_FIELD
import community.accounts.AccountRepository
import community.accounts.signup.logResetAttempt
import community.accounts.validate
import community.core.http.badResponse
import community.core.logging.d
import community.validationProblems
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ServerWebExchange
import java.time.Instant.now

@Service
@Transactional
open class PasswordService(private val accountRepository: AccountRepository) {

    suspend fun getAccountByEmail(email: String) = accountRepository.findOne(email)

    suspend fun reset(email: String, exchange: ServerWebExchange? = null)
            : Triple<ResponseEntity<ProblemDetail>, Account?, String?/*:key*/> {
//    suspend fun requestPasswordReset(mail: String): Account? =
//        accountRepository
//            .findOne(mail)
//            .apply {
//                if (this != null && this.activated) {
//                   copy(resetKey = generateResetKey,
//                    resetDate = now())
//                    saveUser(this)
//                } else return null
//            }

//        val errors = account
//            .validate(signupFields, exchange)
//            .apply { account.logSignupAttempt }


        val errors = Account(email = email)
            .apply { logResetAttempt }
            .validate(setOf(EMAIL_FIELD), exchange)

        val problems = validationProblems.copy(
            path = "$API_ACCOUNT$API_RESET_INIT"
        ).run {
            if (errors.isNotEmpty())
                return Triple(
                    badResponse(errors),
                    null,
                    null
                )
        }

//        val account = accountRepository
//            .findOne(email)
//            .apply {
//
//                if (this != null && this.activated) {
//                    copy(//TODO: repo.generateResetKey(account)
//                        resetKey = generateResetKey,
//                        resetDate = now()
//                    )
//                    saveUser(this)
//                } else return null
//
//            }

        accountRepository.generateResetKey(email).run {

        }
        val result = ResponseEntity<ProblemDetail>(OK)


        return Triple(result ,null,null)

    }


    suspend fun change(currentPassword: String, newPassword: String) {
        TODO("Not yet implemented")
    }

    suspend fun complete(newPassword: String, key: String): Account? =
        accountRepository.findOneByResetKey(key).run {
            if (this != null && second.resetDate?.isAfter(now().minusSeconds(86400)) == true) {
                d("Reset account password for reset key $key")
                return@complete first
                //                return saveUser(
                //                apply {
                ////                    password = passwordEncoder.encode(newPassword)
                //                    resetKey = null
                //                    resetDate = null
                //                })
            } else {
                d("$key is not a valid reset account password key")
                return@complete null
            }
        }
}
 */
