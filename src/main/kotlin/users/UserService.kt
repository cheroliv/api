package users

import arrow.core.Either
import users.signup.Signup

interface UserService {
    suspend fun signupService(signup: Signup): Either<Throwable, User>

    suspend fun activateService(key: String): Long

    /**
     * @return Triple<Boolean/*OK*/, Boolean/*email*/, Boolean/*login*/>
     * first for OK: email and login are valid
     * second for email: email is valid
     * third for login: login is valid
     */
    suspend fun signupAvailability(signup: Signup): Either<Throwable, Triple<Boolean, Boolean, Boolean>>
}