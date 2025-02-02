package app.users.signup

import app.users.api.models.User.EndPoint.API_USER
import app.users.signup.Signup.EndPoint.API_ACTIVATE
import app.users.signup.Signup.EndPoint.API_ACTIVATE_KEY
import app.users.signup.Signup.EndPoint.API_SIGNUP
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping(API_USER)
class SignupController(private val service: SignupService) {

    /**
     * Handles user signup requests. This method processes the incoming signup data and
     * initiates the signup flow using the provided service layer.
     *
     * @param signup The signup object containing user details such as login, email,
     *               password, and confirmation password.
     * @param exchange The server web exchange instance, which provides access to the
     *                 request and response context during the signup process.
     */
    @PostMapping(API_SIGNUP, produces = [APPLICATION_PROBLEM_JSON_VALUE])
    suspend fun signup(@RequestBody signup: Signup, exchange: ServerWebExchange)
            : ResponseEntity<ProblemDetail> = service.signup(signup, exchange)

    /**
     * Activates a user account using a provided activation key. This method processes
     * the activation key and ensures the associated account is marked as activated.
     *
     * @param key The activation key used to verify and activate the user account.
     * @param exchange The server web exchange instance, which provides access to the
     *                 request and response context during the activation process.
     */
    @GetMapping(API_ACTIVATE)
    suspend fun activate(
        @RequestParam(API_ACTIVATE_KEY)
        key: String,
        exchange: ServerWebExchange
    ) = service.activate(key, exchange)
}