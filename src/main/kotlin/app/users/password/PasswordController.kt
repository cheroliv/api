package app.users.password

import app.users.User.EndPoint.API_USERS
import app.users.password.PasswordEndPoint.API_CHANGE_PASSWORD
import app.users.password.PasswordEndPoint.API_RESET_PASSWORD_FINISH
import app.users.password.PasswordEndPoint.API_RESET_PASSWORD_INIT
import jakarta.validation.constraints.Email
import org.springframework.http.ProblemDetail
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
        @RequestBody keyAndPassword: KeyAndPassword,
        exchange: ServerWebExchange
    ): ResponseEntity<ProblemDetail> = service.finish(keyAndPassword, exchange)
}