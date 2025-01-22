package app.users.core.dao

import app.users.core.Constants
import app.users.core.dao.UserRoleDao.signup
import app.users.core.models.EntityModel
import app.users.core.models.Role
import app.users.core.models.User
import app.users.core.models.User.Attributes.EMAIL_ATTR
import app.users.core.models.User.Attributes.EMAIL_OR_LOGIN
import app.users.core.models.User.Attributes.ID_ATTR
import app.users.core.models.User.Attributes.LANG_KEY_ATTR
import app.users.core.models.User.Attributes.LOGIN_ATTR
import app.users.core.models.User.Attributes.PASSWORD_ATTR
import app.users.core.models.User.Attributes.VERSION_ATTR
import app.users.core.models.User.Members.ROLES_MEMBER
import app.users.core.models.User.Relations.EMAIL_AVAILABLE_COLUMN
import app.users.core.models.User.Relations.FIND_USER_WITH_AUTHS_BY_EMAILOGIN
import app.users.core.models.User.Relations.Fields.EMAIL_FIELD
import app.users.core.models.User.Relations.Fields.ID_FIELD
import app.users.core.models.User.Relations.Fields.LANG_KEY_FIELD
import app.users.core.models.User.Relations.Fields.LOGIN_FIELD
import app.users.core.models.User.Relations.Fields.PASSWORD_FIELD
import app.users.core.models.User.Relations.Fields.VERSION_FIELD
import app.users.core.models.User.Relations.INSERT
import app.users.core.models.User.Relations.LOGIN_AND_EMAIL_AVAILABLE_COLUMN
import app.users.core.models.User.Relations.LOGIN_AVAILABLE_COLUMN
import app.users.core.models.User.Relations.SELECT_SIGNUP_AVAILABILITY
import app.users.core.models.User.Relations.UPDATE_PASSWORD
import app.users.core.models.UserRole
import app.users.signup.Signup
import app.users.signup.SignupDao.save
import app.users.signup.UserActivation
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import jakarta.validation.Validator
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.core.publisher.Mono
import java.lang.Boolean.parseBoolean
import java.util.UUID

object UserDao {
    //@Configuration
//class UserDaoConfig(val context:ApplicationContext) : AbstractR2dbcConfiguration() {
//    override fun connectionFactory(): ConnectionFactory {
//        return context.getBean<ConnectionFactory>()
//    }
//}

    @Throws(EmptyResultDataAccessException::class)
    suspend fun Pair<User, ApplicationContext>.save(): Either<Throwable, UUID> = try {
        INSERT
            .trimIndent()
            .run(second.getBean<DatabaseClient>()::sql)
            .bind(LOGIN_ATTR, first.login)
            .bind(EMAIL_ATTR, first.email)
            .bind(PASSWORD_ATTR, first.password)
            .bind(LANG_KEY_ATTR, first.langKey)
            .bind(VERSION_ATTR, first.version)
            .fetch()
            .awaitOne()[ID_ATTR]
            .toString()
            .run(UUID::fromString)
            .right()
    } catch (e: Throwable) {
        e.left()
    }


    suspend inline fun <reified T : EntityModel<UUID>> ApplicationContext.findOne(
        emailOrLogin: String
    ): Either<Throwable, User> = when (T::class) {
        User::class -> {
            try {
                when {
                    !((emailOrLogin to this).run {
                        second.getBean<Validator>()
                            .validateValue(User::class.java, EMAIL_ATTR, first)
                            .isEmpty()
                    } || (emailOrLogin to this).run {
                        second.getBean<Validator>()
                            .validateValue(User::class.java, LOGIN_ATTR, first)
                            .isEmpty()
                    }) -> "not a valid login or not a valid email"
                        .run(::Exception)
                        .left()

                    else -> FIND_USER_WITH_AUTHS_BY_EMAILOGIN
                        .trimIndent()
                        .run(getBean<DatabaseClient>()::sql)
                        .bind(EMAIL_OR_LOGIN, emailOrLogin)
                        .fetch()
                        .awaitSingleOrNull()
                        .run {
                            when {
                                this == null -> Exception("not able to retrieve user id and roles").left()
                                else -> User(
                                    id = UUID.fromString(get(ID_FIELD).toString()),
                                    email = get(EMAIL_FIELD).toString(),
                                    login = get(LOGIN_FIELD).toString(),
                                    roles = get(ROLES_MEMBER)
                                        .toString()
                                        .split(",")
                                        .map { Role(it) }
                                        .toSet(),
                                    password = get(PASSWORD_FIELD).toString(),
                                    langKey = get(LANG_KEY_FIELD).toString(),
                                    version = get(VERSION_FIELD).toString().toLong(),
                                ).right()
                            }
                        }
                }
            } catch (e: Throwable) {
                e.left()
            }
        }

        else -> (T::class.simpleName)
            .run { "Unsupported type: $this" }
            .run(::IllegalArgumentException)
            .left()
    }


    @Throws(EmptyResultDataAccessException::class)
    suspend fun Pair<User, ApplicationContext>.signup(): Either<Throwable, Pair<UUID, String>> =
        try {
            (first.copy(
                password = second.getBean<PasswordEncoder>().encode(first.password)
            ) to second).save()
            second.findOne<User>(first.email).mapLeft {
                return Exception("Unable to find user by email").left()
            }.map {
                when {
                    it.id != null -> {
                        (UserRole(userId = it.id, role = Constants.ROLE_USER) to second).signup()
                        UserActivation(id = it.id).run {
                            (this to second).save()
                            return (it.id to activationKey).right()
                        }
                    }

                    else -> return Exception("Unable to find user by email").left()
                }
            }
        } catch (e: Throwable) {
            e.left()
        }

    fun ApplicationContext.user(signup: Signup): User = signup.apply {
        // Validation du mot de passe et de la confirmation
        require(password == repassword) { "Passwords do not match!" }
    }.run {
        // Création d'un utilisateur à partir des données de Signup
        User(
            login = login,
            password = getBean<PasswordEncoder>().encode(password),
            email = email,
        )
    }

    suspend fun Pair<Signup, ApplicationContext>.availability()
            : Either<Throwable, Triple<Boolean/*OK*/, Boolean/*email*/, Boolean/*login*/>> = try {
        SELECT_SIGNUP_AVAILABILITY
            .trimIndent()
            .run(second.getBean<DatabaseClient>()::sql)
            .bind(LOGIN_ATTR, first.login)
            .bind(EMAIL_ATTR, first.email)
            .fetch()
            .awaitSingle()
            .run {
                Triple(
                    parseBoolean(this[LOGIN_AND_EMAIL_AVAILABLE_COLUMN].toString()),
                    parseBoolean(this[EMAIL_AVAILABLE_COLUMN].toString()),
                    parseBoolean(this[LOGIN_AVAILABLE_COLUMN].toString())
                ).right()
            }
    } catch (e: Throwable) {
        e.left()
    }

    @Throws(EmptyResultDataAccessException::class)
    suspend fun Pair<User, ApplicationContext>.change()
            : Either<Throwable, Long> = try {
        UPDATE_PASSWORD
            .trimIndent()
            .run(second.getBean<DatabaseClient>()::sql)
            .bind(ID_ATTR, first.id!!)
            .bind(PASSWORD_ATTR, first.password.run(second.getBean<PasswordEncoder>()::encode))
            .bind(VERSION_ATTR, first.version)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
            .right()
    } catch (e: Throwable) {
        e.left()
    }

    fun ApplicationContext.userDetails(
        emailOrLogin: String
    ): Mono<UserDetails> = getBean<Validator>().run {
        when {
            validateProperty(
                User(email = emailOrLogin),
                EMAIL_FIELD
            ).isNotEmpty() && validateProperty(
                User(login = emailOrLogin),
                LOGIN_FIELD
            ).isNotEmpty() -> throw UsernameNotFoundException("User $emailOrLogin was not found")

            else -> mono {
                findOne<User>(emailOrLogin).map { user ->
                    return@mono org.springframework.security.core.userdetails.User(
                        user.login,
                        user.password,
                        user.roles.map { SimpleGrantedAuthority(it.id) })
                }.getOrNull() ?: throw UsernameNotFoundException("User $emailOrLogin was not found")
            }
        }
    }
}