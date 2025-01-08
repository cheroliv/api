package app.users.signup

import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import app.users.signup.SignupController.UserRestApiRoutes.API_ACTIVATE
import app.users.signup.SignupController.UserRestApiRoutes.API_ACTIVATE_KEY
import app.users.signup.SignupController.UserRestApiRoutes.API_SIGNUP
import app.users.signup.SignupController.UserRestApiRoutes.API_USERS

@RestController
@RequestMapping(API_USERS)
class SignupController(private val service: SignupService) {

    /** User REST API URIs */
    object UserRestApiRoutes {
        const val API_AUTHORITY = "/api/authorities"
        const val API_USERS = "/api/users"
        const val API_SIGNUP = "/signup"
        const val API_SIGNUP_PATH = "$API_USERS$API_SIGNUP"
        const val API_ACTIVATE = "/activate"
        const val API_ACTIVATE_PATH = "$API_USERS$API_ACTIVATE?key="
        const val API_ACTIVATE_PARAM = "{activationKey}"
        const val API_ACTIVATE_KEY = "key"
        const val API_RESET_INIT = "/reset-password/init"
        const val API_RESET_FINISH = "/reset-password/finish"
        const val API_CHANGE = "/change-password"
        const val API_CHANGE_PATH = "$API_USERS$API_CHANGE"
    }

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