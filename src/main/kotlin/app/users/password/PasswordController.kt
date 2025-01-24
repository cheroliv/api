package app.users.password

import app.users.core.models.User.EndPoint.API_USER
import app.users.password.UserReset.EndPoint.API_CHANGE_PASSWORD
import app.users.password.UserReset.EndPoint.API_RESET_PASSWORD_FINISH
import app.users.password.UserReset.EndPoint.API_RESET_PASSWORD_INIT
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange


@RestController
@RequestMapping(API_USER)
class PasswordController(private val service: PasswordService) {

    @PostMapping(API_CHANGE_PASSWORD)
    suspend fun change(
        @RequestBody
        passwordChange: PasswordChange,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = service.change(passwordChange, exchange)

    @PostMapping(API_RESET_PASSWORD_INIT)
    suspend fun reset(
        @RequestBody @Email mail: String,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = service.reset(mail, exchange)

    @PostMapping(API_RESET_PASSWORD_FINISH)
    suspend fun finish(
        @RequestBody @Valid resetPassword: ResetPassword,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = service.finish(resetPassword, exchange)
}