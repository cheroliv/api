package app.users.password

import app.users.User.EndPoint.API_USERS
import app.users.password.PasswordEndPoint.API_CHANGE_PASSWORD
import app.users.password.PasswordEndPoint.API_RESET_PASSWORD_INIT
import jakarta.validation.constraints.Email
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange


@RestController
@RequestMapping(API_USERS)
class PasswordController(private val service: PasswordService) {

    @PostMapping(API_CHANGE_PASSWORD)
    suspend fun change(
        @RequestBody passwordChange: PasswordChange,
        exchange: ServerWebExchange
    ) = service.change(passwordChange, exchange)

    @PostMapping(API_RESET_PASSWORD_INIT)
    suspend fun reset(
        @RequestBody @Email mail: String,
        exchange: ServerWebExchange
    ) = service.reset(mail, exchange)

//    /**
//     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
//     *
//     * @param keyAndPassword the generated key and the new password.
//     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
//     * @throws RuntimeException         {@code 500 (Internal Application Error)} if the password could not be reset.
//     */
//    @PostMapping(RESET_PASSWORD_API_FINISH)//TODO: retourner des problemDetails
//    suspend fun finishPasswordReset(@RequestBody keyAndPassword: KeyAndPassword): Unit =
//        InvalidPasswordException().run {
//            when {
//                validator
//                    .validateProperty(
//                        AccountCredentials(password = keyAndPassword.newPassword),
//                        PASSWORD_FIELD
//                    ).isNotEmpty() -> throw this
//                keyAndPassword.newPassword != null
//                        && keyAndPassword.key != null
//                        && passwordService.completePasswordReset(
//                    keyAndPassword.newPassword,
//                    keyAndPassword.key
//                ) == null -> throw PasswordException("No user was found for this reset key")
//            }
//        }
//
}

/**
@file:Suppress("unused")

package community.accounts.password

import community.*
import community.accounts.Account
import community.accounts.Account.Companion.PASSWORD_FIELD
import community.accounts.InvalidPasswordException
import community.accounts.mail.MailService
import community.core.http.badResponse
import community.accounts.signup.Signup
import jakarta.validation.Validator
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

/*=================================================================================*/
@RestController
@RequestMapping(API_ACCOUNT)
class PasswordController(
private val passwordService: PasswordService,
private val mailService: MailService,
private val validator: Validator
) {
internal class PasswordException(message: String) : RuntimeException(message)

/**
 * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
 *
 * @param email the email of the user.
*/
@PostMapping(API_RESET_INIT)//TODO: retourner des problemDetails
// et virer la validation dans la signature
suspend fun requestPasswordReset(
@RequestBody email: String,
exchange: ServerWebExchange
): ResponseEntity<ProblemDetail> = passwordService
.reset(email, exchange)
.apply {
when (first.statusCode) {
OK -> mailService.sendPasswordResetMail(second!!)
BAD_REQUEST -> w("Password reset requested for non existing email")
else -> w("Password reset request caused unknoww error")
}
}.first




/**
 * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
 *
 * @param passwordReset the generated key and the new password.
 * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
 * @throws RuntimeException         {@code 500 (Internal Application Error)} if the password could not be reset.
*/
@PostMapping(API_RESET_FINISH)//TODO: retourner des problemDetails
suspend fun finishPasswordReset(@RequestBody passwordReset: PasswordReset): Unit =
InvalidPasswordException().run {
when {
validator.validateProperty(
Signup(Account(), passwordReset.newPassword),
PASSWORD_FIELD
).isNotEmpty() -> throw this

passwordReset.newPassword != null
&& passwordReset.key != null
&& passwordService.complete(
passwordReset.newPassword,
passwordReset.key
) == null -> throw PasswordException("No user was found for this reset key")
}
}
}*/