package app.users.password

import app.core.Loggers.d
import app.core.Loggers.i
import app.core.security.SecurityUtils.getCurrentUserLogin
import app.core.web.HttpUtils.validator
import app.users.User
import app.users.UserDao.change
import app.users.UserDao.findOne
import app.users.password.PasswordChange.Attributes.NEW_PASSWORD_ATTR
import arrow.core.getOrElse
import jakarta.validation.Valid
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.server.ServerWebExchange

@Service
@Validated
class PasswordService(val context: ApplicationContext) {

    suspend fun change(@Valid changePassword: PasswordChange): Long {
        getCurrentUserLogin().apply {
            d("Current security context user.login : $this")
            when {
                isNotBlank() -> {
                    // TODO: use findOne instead, but it needs to be fixed!!!
                    context.findOne<User>(this).map {
                        when {
                            context.getBean<PasswordEncoder>().matches(
                                changePassword.currentPassword,
                                it.password
                            ) -> return (it.copy(password = changePassword.newPassword) to context).change()
                                //invalid update password persistence
                                .getOrElse { throw InvalidPasswordException() }
                                .apply { d("Changed password for User: ${it.login}") }

                            else -> throw InvalidPasswordException()//invalid security context login
                        }
                    }
                }
            }
        }
        throw InvalidPasswordException()//invalid security authorization
    }

    suspend fun change(
        passwordChange: PasswordChange,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = exchange
        .validator
        .validateProperty(
            passwordChange,
            NEW_PASSWORD_ATTR
        ).run {
            when {
                isNotEmpty() -> ResponseEntity.of(
                    forStatusAndDetail(BAD_REQUEST, iterator().next().message)
                )

                else -> try {
                    change(passwordChange)
                    ResponseEntity.ok()
                } catch (t: Throwable) {
                    ResponseEntity.of(forStatusAndDetail(BAD_REQUEST, t.message))
                }
            }
        }.build()

    suspend fun reset(mail: String, exchange: ServerWebExchange)
            : ResponseEntity<ProblemDetail> = try {
        with(requestPasswordReset(mail)) {
            when {
                this == null -> ResponseEntity.of(
                    forStatusAndDetail(
                        BAD_REQUEST,
                        "Password reset requested for non existing mail"
                    )
                )

                else -> apply(::sendPasswordResetMail)
                    .run { ResponseEntity.ok() }
            }
        }
    } catch (t: Throwable) {
        ResponseEntity.of(forStatusAndDetail(BAD_REQUEST, t.message))
    }.build()

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

    suspend fun requestPasswordReset(mail: String): User? = null

    fun sendPasswordResetMail(user: User) {
        i("Not yet implemented")
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

        //TODO: renvoi une ValidationViolationException ou une InvalidPasswordException ou OK
    //    suspend fun changePassword(currentClearTextPassword: String, newPassword: String) {
    //        securityUtils.getCurrentUserLogin().apply {
    //            if (!isNullOrBlank()) {
    //                userRepository.findOneByLogin(this).apply {
    //                    if (this != null) {
    //                        if (!passwordEncoder.matches(
    //                                currentClearTextPassword,
    //                                password
    //                            )
    //                        ) throw InvalidPasswordException()
    //                        else saveUser(this.apply {
    //                            password = passwordEncoder.encode(newPassword)
    //                        }).run {
    //                            d("Changed password for User: {}", this)
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //    }
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
}