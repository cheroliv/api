package app.users.signup

import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import app.users.signup.SignupEndPoint.API_ACTIVATE
import app.users.signup.SignupEndPoint.API_ACTIVATE_KEY
import app.users.signup.SignupEndPoint.API_SIGNUP
import app.users.User.EndPoint.API_USERS

@RestController
@RequestMapping(API_USERS)
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