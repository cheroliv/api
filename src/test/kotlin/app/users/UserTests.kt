@file:Suppress(
    "NonAsciiCharacters",
    "SqlResolve",
    "RedundantUnitReturnType","unused"
)

package app.users
//import jakarta.mail.Multipart
//import jakarta.mail.internet.MimeBodyPart
//import jakarta.mail.internet.MimeMessage
//import jakarta.mail.internet.MimeMultipart
//import org.assertj.base.api.Assertions
//import org.assertj.base.api.Assertions.assertThat
//import org.junit.jupiter.api.AfterAll
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.BeforeEach
//import org.mockito.ArgumentCaptor
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Captor
//import org.mockito.Mockito
//import org.mockito.Mockito.*
//import org.mockito.MockitoAnnotations.openMocks
//import org.mockito.Spy
//import org.springframework.beans.factory.getBean
//import org.springframework.context.ConfigurableApplicationContext
//import org.springframework.context.MessageSource
//import org.springframework.mail.MailSendException
//import org.springframework.mail.javamail.JavaMailSenderImpl
//import org.thymeleaf.spring6.SpringWebFluxTemplateEngine
//import school.accounts.models.AccountCredentials
//import school.accounts.models.AccountUtils.generateResetKey
//import school.base.mail.MailService
//import school.base.mail.MailServiceSmtp
//import school.base.property.DEFAULT_LANGUAGE
//import school.base.property.GMAIL
//import school.launcher
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.FileInputStream
//import java.io.InputStreamReader
//import java.net.URI
//import java.nio.charset.Charset
//import java.util.*
//import java.util.regex.Pattern
//import java.util.regex.Pattern.compile
//import kotlin.test.Ignore
//import kotlin.test.Test
//import kotlin.test.assertNotNull

import app.API
import app.database.EntityModel.Companion.MODEL_FIELD_FIELD
import app.database.EntityModel.Companion.MODEL_FIELD_MESSAGE
import app.database.EntityModel.Companion.MODEL_FIELD_OBJECTNAME
import app.database.EntityModel.Members.withId
import app.http.HttpUtils.validator
import app.workspace.Tools.logBody
import app.workspace.Tools.responseToString
import app.users.User.Attributes.EMAIL_ATTR
import app.users.User.Attributes.LOGIN_ATTR
import app.users.User.Attributes.PASSWORD_ATTR
import app.users.User.Fields.LOGIN_FIELD
import app.users.User.Relations.FIND_ALL_USERS
import app.users.User.Relations.FIND_USER_BY_LOGIN
import app.users.UserController.UserRestApiRoutes.API_ACTIVATE_PARAM
import app.users.UserController.UserRestApiRoutes.API_ACTIVATE_PATH
import app.users.UserController.UserRestApiRoutes.API_SIGNUP_PATH
import app.users.UserDao.countUsers
import app.users.UserDao.delete
import app.users.UserDao.deleteAllUsersOnly
import app.users.UserDao.findOne
import app.users.UserDao.findOneByEmail
import app.users.UserDao.findOneWithAuths
import app.users.UserDao.save
import app.users.UserDao.signupAvailability
import app.users.UserDao.signupDao
import app.users.UserService.Companion.SIGNUP_AVAILABLE
import app.users.UserService.Companion.SIGNUP_EMAIL_NOT_AVAILABLE
import app.users.UserService.Companion.SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE
import app.users.UserService.Companion.SIGNUP_LOGIN_NOT_AVAILABLE
import app.users.UserService.Companion.validate
import app.users.Utils.Data.DEFAULT_USER_JSON
import app.users.Utils.Data.OFFICIAL_SITE
import app.users.Utils.Data.admin
import app.users.Utils.Data.signup
import app.users.Utils.Data.user
import app.users.Utils.Data.users
import app.users.Utils.defaultRoles
import app.users.Utils.findAuthsByEmail
import app.users.Utils.findAuthsByLogin
import app.users.Utils.findUserActivationByKey
import app.users.Utils.findUserById
import app.users.Utils.tripleCounts
import app.users.security.Role
import app.users.security.RoleDao.countRoles
import app.users.security.UserRole
import app.users.security.UserRoleDao.countUserAuthority
import app.users.signup.Signup
import app.users.signup.Signup.Companion.objectName
import app.users.signup.UserActivation
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Companion.ACTIVATION_KEY_SIZE
import app.users.signup.UserActivation.Fields.ACTIVATION_DATE_FIELD
import app.users.signup.UserActivation.Fields.ACTIVATION_KEY_FIELD
import app.users.signup.UserActivation.Fields.CREATED_DATE_FIELD
import app.users.signup.UserActivation.Relations.FIND_ALL_USERACTIVATION
import app.users.signup.UserActivation.Relations.FIND_BY_ACTIVATION_KEY
import app.users.signup.UserActivationDao.activateDao
import app.users.signup.UserActivationDao.countUserActivation
import app.Utils.lsWorkingDir
import app.Utils.lsWorkingDirProcess
import app.Utils.toJson
import app.Constants.DEVELOPMENT
import app.Constants.EMPTY_STRING
import app.Constants.PRODUCTION
import app.Constants.ROLE_USER
import app.Constants.STARTUP_LOG_MSG_KEY
import app.Constants.VIRGULE
import app.Properties
import app.workspace.Log.i
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Validation.byProvider
import jakarta.validation.Validator
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils.random
import org.hibernate.validator.HibernateValidator
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.web.server.ServerWebExchange
import java.io.File
import java.nio.file.Paths
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.*
import java.util.Locale.*
import java.util.UUID.fromString
import java.util.UUID.randomUUID
import javax.inject.Inject
import kotlin.test.*
import app.mail.MailConfiguration.GoogleAuthConfig

@ActiveProfiles("test")
@SpringBootTest(
    classes = [API::class],
    properties = ["spring.main.web-application-type=reactive"]
)
class UserTests {

    @Inject
    lateinit var context: ApplicationContext
    lateinit var client: WebTestClient

    @BeforeTest
    fun setUp(context: ApplicationContext) {
        client = context.run(WebTestClient::bindToApplicationContext).build()
    }

    @AfterTest
    fun cleanUp(context: ApplicationContext) = runBlocking { context.deleteAllUsersOnly() }

    @Test
    fun `DataTestsChecks - display some json`(): Unit = run {
        assertDoesNotThrow {
            context.getBean<ObjectMapper>().run {
                writeValueAsString(users).run(::i)
                writeValueAsString(user).run(::i)
            }
            DEFAULT_USER_JSON.run(::i)
        }
    }

    @Test
    fun `ConfigurationsTests - MessageSource test email_activation_greeting message fr`(): Unit =
        "artisan-logiciel".run {
            assertEquals(
                expected = "Cher $this",
                actual = context
                    .getBean<MessageSource>()
                    .getMessage(
                        "email.activation.greeting",
                        arrayOf(this),
                        FRENCH
                    )
            )
        }


    @Test
    fun `ConfigurationsTests - MessageSource test message startupLog`(): Unit = context
        .getBean<MessageSource>()
        .getMessage(
            STARTUP_LOG_MSG_KEY,
            arrayOf(DEVELOPMENT, PRODUCTION),
            getDefault()
        ).run {
            i(this)
            assertEquals(buildString {
                append("You have misconfigured your application!\n")
                append("It should not run with both the $DEVELOPMENT\n")
                append("and $PRODUCTION profiles at the same time.")
            }, this)
        }


    @Test
    fun `ConfigurationsTests - test go visit message`(): Unit = assertEquals(
        OFFICIAL_SITE,
        context.getBean<Properties>().goVisitMessage
    )

    @Test
    fun `test lsWorkingDir & lsWorkingDirProcess`(): Unit = "build".let {
        it.run(::File).run {
            context
                .lsWorkingDirProcess(this)
                .run { "lsWorkingDirProcess : $this" }
                .run(::i)
            absolutePath.run(::i)
            // Liste un répertoire spécifié par une chaîne
            context.lsWorkingDir(it, maxDepth = 2)
            // Liste un répertoire spécifié par un Path
            context.lsWorkingDir(Paths.get(it))
        }
    }


    @Test
    fun `display user formatted in JSON`(): Unit = assertDoesNotThrow {
        (user to context).toJson.let(::i)
    }

    @Test
    fun `check toJson build a valid json format`(): Unit = assertDoesNotThrow {
        (user to context)
            .toJson
            .let(context.getBean<ObjectMapper>()::readTree)
    }

    @Test
    fun `test signup and trying to retrieve the user id from databaseClient object`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        (user to context).signupDao().onRight {
            //Because 36 == UUID.toString().length
            it.toString().apply { assertEquals(36, it.first.toString().length) }.apply(::i)
        }
        assertEquals(1, context.countUsers())
        assertDoesNotThrow {
            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch()
                .all()
                .collect {
                    it[User.Fields.ID_FIELD]
                        .toString()
                        .run(UUID::fromString)
                }
        }
    }

    @Test
    fun `test findOneWithAuths using one query`(): Unit = runBlocking {
        context.tripleCounts().run {
            assertEquals(Triple(0, 0, 0), this)
            (user to context).signupDao()
            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserAuthority())
            assertEquals(third + 1, context.countUserActivation())
        }
        """
            SELECT 
               u."id",
               u."email",
               u."login",
               u."password",
               u."lang_key",
               u."version",
               STRING_AGG(DISTINCT a."role", ',') AS roles
            FROM "user" AS u
            LEFT JOIN 
               user_authority ua ON u."id" = ua."user_id"
            LEFT JOIN 
               authority AS a ON ua."role" = a."role"
            WHERE 
               LOWER(u."email") = LOWER(:emailOrLogin) 
               OR 
               LOWER(u."login") = LOWER(:emailOrLogin)
            GROUP BY 
               u."id", u."email", u."login";"""
            .trimIndent()
            .apply(::i)
            .run(context.getBean<DatabaseClient>()::sql)
            .bind("emailOrLogin", user.email)
            .fetch()
            .awaitSingleOrNull()
            ?.run {
                toString().run(::i)
                val expectedUserResult = User(
                    id = fromString(get(User.Fields.ID_FIELD).toString()),
                    email = get(User.Fields.EMAIL_FIELD).toString(),
                    login = get(LOGIN_FIELD).toString(),
                    roles = get(User.Members.ROLES_MEMBER)
                        .toString()
                        .split(",")
                        .map { Role(it) }
                        .toSet(),
                    password = get(User.Fields.PASSWORD_FIELD).toString(),
                    langKey = get(User.Fields.LANG_KEY_FIELD).toString(),
                    version = get(User.Fields.VERSION_FIELD).toString().toLong(),
                )
                val userResult = context
                    .findOneWithAuths<User>(user.login)
                    .getOrNull()!!
                assertNotNull(expectedUserResult)
                assertNotNull(expectedUserResult.id)
                assertTrue(expectedUserResult.roles.isNotEmpty())
                assertEquals(expectedUserResult.roles.first().id, ROLE_USER)
                assertEquals(1, expectedUserResult.roles.size)
                assertEquals(expectedUserResult, userResult)
                assertEquals(
                    user.withId(expectedUserResult.id!!)
                        .copy(roles = setOf(Role(ROLE_USER))),
                    userResult
                )
            }
    }

    @Test
    fun `test findOneWithAuths`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        assertEquals(0, context.countUserAuthority())
        val userId: UUID = (user to context).signupDao().getOrNull()!!.first
        userId.apply { run(::assertNotNull) }
            .run { "(user to context).signup() : $this" }
            .run(::println)
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
        context.findOneWithAuths<User>(user.email)
            .getOrNull()
            ?.apply {
                run(::assertNotNull)
                assertEquals(1, roles.size)
                assertEquals(ROLE_USER, roles.first().id)
                assertEquals(userId, id)
            }.run { "context.findOneWithAuths<User>(${user.email}).getOrNull() : $this" }
            .run(::println)
        context.findOne<User>(user.email).getOrNull()
            .run { "context.findOne<User>(user.email).getOrNull() : $this" }
            .run(::println)
        context.findAuthsByEmail(user.email).getOrNull()
            .run { "context.findAuthsByEmail(${user.email}).getOrNull() : $this" }
            .run(::println)
    }

    @Test
    fun `test findOne`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        (user to context).save()
        assertEquals(1, context.countUsers())
        val findOneEmailResult: Either<Throwable, User> = context.findOne<User>(user.email)
        findOneEmailResult.map { assertDoesNotThrow { fromString(it.toString()) } }
        println("findOneEmailResult : ${findOneEmailResult.getOrNull()}")
        context.findOne<User>(user.login).map { assertDoesNotThrow { fromString(it.toString()) } }
    }

    @Test
    fun `test findUserById`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        lateinit var userWithAuths: User
        (user to context).signupDao().apply {
            isRight().run(::assertTrue)
            isLeft().run(::assertFalse)
        }.map {
            userWithAuths = user.withId(it.first).copy(password = EMPTY_STRING)
            userWithAuths.roles.isEmpty().run(::assertTrue)
        }
        userWithAuths.id.run(::assertNotNull)
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
        val userResult = context.findUserById(userWithAuths.id!!)
            .getOrNull()
            .apply { run(::assertNotNull) }
            .apply { userWithAuths = userWithAuths.copy(roles = this?.roles ?: emptySet()) }
        (userResult to userWithAuths).run {
            assertEquals(first?.id, second.id)
            assertEquals(first?.roles?.size, second.roles.size)
            assertEquals(first?.roles?.first(), second.roles.first())
        }
        userWithAuths.roles.isNotEmpty().run(::assertTrue)
        assertEquals(ROLE_USER, userWithAuths.roles.first().id)
        "userWithAuths : $userWithAuths".run(::println)
        "userResult : $userResult".run(::println)
    }

    @Test
    fun `test findAuthsByLogin`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        lateinit var userWithAuths: User
        (user to context).signupDao().apply {
            isRight().run(::assertTrue)
            isLeft().run(::assertFalse)
        }.map {
            userWithAuths = user.withId(it.first).copy(password = EMPTY_STRING)
            userWithAuths.roles.isEmpty().run(::assertTrue)
        }
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
        context.findAuthsByLogin(user.login)
            .getOrNull()
            .apply { run(::assertNotNull) }
            .run { userWithAuths = userWithAuths.copy(roles = this!!) }
        userWithAuths.roles.isNotEmpty().run(::assertTrue)
        assertEquals(ROLE_USER, userWithAuths.roles.first().id)
        "userWithAuths : $userWithAuths".run(::println)
    }

    @Test
    fun `test findAuthsByEmail`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        lateinit var userWithAuths: User
        (user to context).signupDao().apply {
            isRight().run(::assertTrue)
            isLeft().run(::assertFalse)
        }.map {
            userWithAuths = user.withId(it.first).copy(password = EMPTY_STRING)
            userWithAuths.roles.isEmpty().run(::assertTrue)
        }
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
        context.findAuthsByEmail(user.email)
            .getOrNull()
            .apply { run(::assertNotNull) }
            .run { userWithAuths = userWithAuths.copy(roles = this!!) }
        userWithAuths.roles.isNotEmpty().run(::assertTrue)
        assertEquals(ROLE_USER, userWithAuths.roles.first().id)
        "userWithAuths : $userWithAuths".run(::println)
    }

    @Test
    fun `test findOneWithAuths with existing email login and roles`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        (user to context).signupDao()
        val resultRoles = mutableSetOf<Role>()
        context.findAuthsByEmail(user.email).run {
            resultRoles.addAll(map { it }.getOrElse { emptySet() })
        }
        assertEquals(ROLE_USER, resultRoles.first().id)
        assertEquals(ROLE_USER, resultRoles.first().id)
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
    }

    @Test
    fun `try to do implementation of findOneWithAuths with existing email login and roles using composed query`(): Unit =
        runBlocking {
            val countUserBefore = context.countUsers()
            assertEquals(0, countUserBefore)
            val countUserAuthBefore = context.countUserAuthority()
            assertEquals(0, countUserAuthBefore)
            val resultRoles = mutableSetOf<String>()
            (user to context).signupDao()
            """
            SELECT ua."role" 
            FROM "user" u 
            JOIN user_authority ua 
            ON u.id = ua.user_id 
            WHERE u."email" = :email;"""
                .trimIndent()
                .run(context.getBean<DatabaseClient>()::sql)
                .bind("email", user.email)
                .fetch()
                .all()
                .collect { rows ->
                    assertEquals(rows[Role.Fields.ID_FIELD], ROLE_USER)
                    resultRoles.add(rows[Role.Fields.ID_FIELD].toString())
                }
            assertEquals(ROLE_USER, resultRoles.first())
            assertEquals(ROLE_USER, resultRoles.first())
            assertEquals(1, context.countUsers())
            assertEquals(1, context.countUserAuthority())
        }

    @Test
    fun `try to do implementation of findOneWithAuths with existing email login and roles`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        val resultRoles = mutableSetOf<Role>()
        val findAuthsAnswer: Any?//= Either<Throwable,Set<String>>()
        lateinit var resultUserId: UUID
        (user to context).signupDao().apply {
            assertTrue(isRight())
            assertFalse(isLeft())
        }.onRight { signupResult ->
            """
            SELECT ur."role" 
            FROM user_authority AS ur 
            WHERE ur.user_id = :userId"""
                .trimIndent()
                .run(context.getBean<DatabaseClient>()::sql)
                .bind(UserRole.Attributes.USER_ID_ATTR, signupResult.first)
                .fetch()
                .all()
                .collect { rows ->
                    assertEquals(rows[Role.Fields.ID_FIELD], ROLE_USER)
                    resultRoles.add(Role(id = rows[Role.Fields.ID_FIELD].toString()))
                }
            assertEquals(
                ROLE_USER,
                user.withId(signupResult.first).copy(
                    roles =
                        resultRoles
                            .map { it.id.run(::Role) }
                            .toMutableSet())
                    .roles.first().id
            )
            resultUserId = signupResult.first
        }
        assertEquals(
            resultUserId.toString().length,
            "85b34d71-ef1d-41e0-acc1-00ab4ee1f932".length
        )//TODO : assertnotthrow with fromStringToUuid
        assertEquals(ROLE_USER, resultRoles.first().id)
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
    }


    @Test
    fun `test UserRoleDao signup with existing user without user_role`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        val userSaveResult = (user to context).save()
        assertEquals(countUserBefore + 1, context.countUsers())
        userSaveResult//TODO: Problem with the either result do not return the user id but persist it on database
            .map { i("on passe ici!") }
            .mapLeft { i("on passe par la!") }

        val userId = context.getBean<DatabaseClient>().sql(FIND_USER_BY_LOGIN)
            .bind(LOGIN_ATTR, user.login.lowercase())
            .fetch()
            .one()
            .awaitSingle()[User.Attributes.ID_ATTR]
            .toString()
            .run(UUID::fromString)

        context.getBean<DatabaseClient>()
            .sql(UserRole.Relations.INSERT)
            .bind(UserRole.Attributes.USER_ID_ATTR, userId)
            .bind(UserRole.Attributes.ROLE_ATTR, ROLE_USER)
            .fetch()
            .one()
            .awaitSingleOrNull()

        """
        SELECT ua.${UserRole.Fields.ID_FIELD} 
        FROM ${UserRole.Relations.TABLE_NAME} AS ua 
        where ua.user_id= :userId and ua."role" = :role"""
            .trimIndent()
            .run(context.getBean<DatabaseClient>()::sql)
            .bind(UserRole.Attributes.USER_ID_ATTR, userId)
            .bind(UserRole.Attributes.ROLE_ATTR, ROLE_USER)
            .fetch()
            .one()
            .awaitSingle()[UserRole.Fields.ID_FIELD]
            .toString()
            .let { "user_role_id : $it" }
            .run(::i)

        assertEquals(countUserAuthBefore + 1, context.countUserAuthority())
    }

    @Test
    fun `check findOneByEmail with non-existing email`(): Unit = runBlocking {
        assertEquals(
            0,
            context.countUsers(),
            "context should not have a user recorded in database"
        )
        context.findOneByEmail<User>("user@dummy.com").apply {
            assertFalse(isRight())
            assertTrue(isLeft())
        }.mapLeft { assertTrue(it is EmptyResultDataAccessException) }
    }

    @Test
    fun `check findOneByEmail with existing email`(): Unit = runBlocking {
        assertEquals(
            0,
            context.countUsers(),
            "context should not have a user recorded in database"
        )
        (user to context).save()
        assertEquals(
            1,
            context.countUsers(),
            "context should have only one user recorded in database"
        )

        context.findOneByEmail<User>(user.email).apply {
            assertTrue(isRight())
            assertFalse(isLeft())
        }.map { assertDoesNotThrow { fromString(it.toString()) } }
    }

    @Test
    fun `test findOne with not existing email or login`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        context.findOne<User>(user.email).apply {
            assertFalse(isRight())
            assertTrue(isLeft())
        }
        context.findOne<User>(user.login).apply {
            assertFalse(isRight())
            assertTrue(isLeft())
        }
    }

    @Test
    fun `save default user should work in this context `(): Unit = runBlocking {
        val count = context.countUsers()
        (user to context).save()
        assertEquals(expected = count + 1, context.countUsers())
    }

    @Test
    fun `test retrieve id from user by existing login`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        (user to context).save()
        assertEquals(countUserBefore + 1, context.countUsers())
        assertDoesNotThrow {
            FIND_USER_BY_LOGIN
                .run(context.getBean<DatabaseClient>()::sql)
                .bind(LOGIN_ATTR, user.login.lowercase())
                .fetch()
                .one()
                .awaitSingle()[User.Attributes.ID_ATTR]
                .toString()
                .run(UUID::fromString)
                .run { i("UserId : $this") }
        }
    }

    @Test
    fun `count users, expected 0`(): Unit = runBlocking {
        assertEquals(
            0,
            context.countUsers(),
            "because init sql script does not inserts default app.users."
        )
    }

    @Test
    fun `count roles, expected 3`(): Unit = runBlocking {
        context.run {
            assertEquals(
                defaultRoles.size,
                countRoles(),
                "Because init sql script does insert default roles."
            )
        }
    }

    @Test
    fun test_deleteAllUsersOnly(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        val countUserAuthBefore = context.countUserAuthority()
        users.forEach { (it to context).signupDao() }
        assertEquals(countUserBefore + 2, context.countUsers())
        assertEquals(countUserAuthBefore + 2, context.countUserAuthority())
        context.deleteAllUsersOnly()
        assertEquals(countUserBefore, context.countUsers())
        assertEquals(countUserAuthBefore, context.countUserAuthority())
    }

    @Test
    fun test_delete(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        val countUserAuthBefore = context.countUserAuthority()
        val ids = users.map { (it to context).signupDao().getOrNull()!! }
        assertEquals(countUserBefore + 2, context.countUsers())
        assertEquals(countUserAuthBefore + 2, context.countUserAuthority())
        ids.forEach { context.delete(it.first) }
        assertEquals(countUserBefore, context.countUsers())
        assertEquals(countUserAuthBefore, context.countUserAuthority())
    }

    @Test
    fun `signupAvailability should return SIGNUP_AVAILABLE for all when login and email are available`(): Unit =
        runBlocking {
            (Signup(
                "testuser",
                "password",
                "password",
                "testuser@example.com"
            ) to context).signupAvailability().run {
                isRight().run(::assertTrue)
                assertEquals(SIGNUP_AVAILABLE, getOrNull()!!)
            }
        }

    @Test
    fun `signupAvailability should return SIGNUP_NOT_AVAILABLE_AGAINST_LOGIN_AND_EMAIL for all when login and email are not available`(): Unit =
        runBlocking {
            assertEquals(0, context.countUsers())
            (user to context).save()
            assertEquals(1, context.countUsers())
            (signup to context).signupAvailability().run {
                assertEquals(
                    SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE,
                    getOrNull()!!
                )
            }
        }

    @Test
    fun `signupAvailability should return SIGNUP_EMAIL_NOT_AVAILABLE when only email is not available`(): Unit =
        runBlocking {
            assertEquals(0, context.countUsers())
            (user to context).save()
            assertEquals(1, context.countUsers())
            (Signup(
                "testuser",
                "password",
                "password",
                user.email
            ) to context).signupAvailability().run {
                assertEquals(SIGNUP_EMAIL_NOT_AVAILABLE, getOrNull()!!)
            }
        }

    @Test
    fun `signupAvailability should return SIGNUP_LOGIN_NOT_AVAILABLE when only login is not available`(): Unit =
        runBlocking {
            assertEquals(0, context.countUsers())
            (user to context).save()
            assertEquals(1, context.countUsers())
            (Signup(
                user.login,
                "password",
                "password",
                "testuser@example.com"
            ) to context).signupAvailability().run {
                assertEquals(SIGNUP_LOGIN_NOT_AVAILABLE, getOrNull()!!)
            }
        }

    @Test
    fun `check signup validate implementation`(): Unit {
        setOf(PASSWORD_ATTR, EMAIL_ATTR, LOGIN_ATTR)
            .map { it to context.getBean<Validator>().validateProperty(signup, it) }
            .flatMap { (first, second) ->
                second.map {
                    mapOf<String, String?>(
                        MODEL_FIELD_OBJECTNAME to objectName,
                        MODEL_FIELD_FIELD to first,
                        MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
            .apply { run(::isEmpty).let(::assertTrue) }
    }

    @Test
    fun `test signup validator with an invalid login`(): Unit = mock<ServerWebExchange>()
        .validator
        .validateProperty(signup.copy(login = "funky-log(n"), LOGIN_ATTR)
        .run {
            assertTrue(isNotEmpty())
            first().run {
                assertEquals(
                    "{${Pattern::class.java.name}.message}",
                    messageTemplate
                )
            }
        }

    @Test
    fun `test signup validator with an invalid password`() {
        val wrongPassword = "123"
        context.getBean<Validator>()
            .validateProperty(signup.copy(password = wrongPassword), PASSWORD_ATTR)
            .run {
                assertTrue(isNotEmpty())
                first().run {
                    assertEquals(
                        "{${Size::class.java.name}.message}",
                        messageTemplate
                    )
                }
            }
    }

    @Test
    fun `Verify that the request contains consistent data`() {
        client
            .post()
            .uri("")
            .contentType(APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .returnResult<Any>()
            .requestBodyContent!!
            .logBody()
            .responseToString()
            .run {
                user.run {
                    mapOf(
                        LOGIN_FIELD to login,
                        User.Fields.PASSWORD_FIELD to password,
                        User.Fields.EMAIL_FIELD to email,
                        //FIRST_NAME_FIELD to firstName,
                        //LAST_NAME_FIELD to lastName,
                    ).map { (key, value) ->
                        assertTrue {
                            contains(key)
                            contains(value)
                        }
                    }
                }
            }
    }

    @Test
    fun `test signup request with an invalid url`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserBefore)
        assertEquals(0, countUserAuthBefore)
        client
            .post()
            .uri("/api/app.users/foobar")
            .contentType(APPLICATION_JSON)
            .bodyValue(signup)
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody()
            .isEmpty
            .responseBodyContent!!
            .logBody()
        assertEquals(countUserBefore, context.countUsers())
        assertEquals(countUserAuthBefore, context.countUserAuthority())
        context.findOneByEmail<User>(user.email).run {
            when (this) {
                is Left -> assertEquals(
                    EmptyResultDataAccessException::class.java,
                    value::class.java
                )

                is Right -> assertEquals(user.id, value)
            }
        }
    }

    @Test
    fun `test signup request with a valid account`(): Unit = runBlocking {
        context.tripleCounts().run {
            client
                .post()
                .uri(API_SIGNUP_PATH)
                .contentType(APPLICATION_JSON)
                .bodyValue(signup)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .isEmpty
            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserAuthority())
            assertEquals(third + 1, context.countUserActivation())
        }
    }


    @Test
    fun `test signup request with an invalid login`() = runBlocking {
        context.run {
            tripleCounts().run {
                client
                    .post()
                    .uri(API_SIGNUP_PATH)
                    .contentType(APPLICATION_PROBLEM_JSON)
                    .header(ACCEPT_LANGUAGE, FRENCH.language)
                    .bodyValue(signup.copy(login = "funky-log(n"))
                    .exchange()
                    .expectStatus()
                    .isBadRequest
                    .returnResult<ProblemDetail>()
                    .responseBodyContent!!
                    .logBody()
                    .isNotEmpty()
                    .run(::assertTrue)
                assertEquals(this, tripleCounts())
            }
        }
    }

    @Test
    fun `test signup with an invalid password`(): Unit = runBlocking {
        val countBefore = context.countUsers()
        assertEquals(0, countBefore)
        client
            .post()
            .uri(API_SIGNUP_PATH)
            .contentType(APPLICATION_PROBLEM_JSON)
            .bodyValue(signup.copy(password = "inv"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<ResponseEntity<ProblemDetail>>()
            .responseBodyContent!!
            .isNotEmpty()
            .run(::assertTrue)
        assertEquals(0, countBefore)
    }

    @Test
    fun `test signup request with an invalid password`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        client
            .post()
            .uri(API_SIGNUP_PATH)
            .contentType(APPLICATION_PROBLEM_JSON)
            .bodyValue(signup.copy(password = "123"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<ResponseEntity<ProblemDetail>>()
            .responseBodyContent!!
            .apply {
                map { it.toInt().toChar().toString() }
                    .reduce { request, s ->
                        request + buildString {
                            append(s)
                            if (s == VIRGULE && request.last().isDigit()) append("\n\t")
                        }
                    }.replace("{\"", "\n{\n\t\"")
                    .replace("\"}", "\"\n}")
                    .replace("\",\"", "\",\n\t\"")
                    .contains(
                        context.getBean<Validator>().validateProperty(
                            signup.copy(password = "123"),
                            "password"
                        ).first().message
                    )
            }.logBody()
            .isNotEmpty()
            .run(::assertTrue)
        assertEquals(0, context.countUsers())
    }

    @Test
    fun `test signup with an existing email`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            context.getBean<UserService>().signupService(signup)
            assertEquals(this@counts.first + 1, context.countUsers())
            assertEquals(this@counts.second + 1, context.countUserAuthority())
            assertEquals(third + 1, context.countUserActivation())
        }
        client
            .post()
            .uri(API_SIGNUP_PATH)
            .contentType(APPLICATION_PROBLEM_JSON)
            .bodyValue(signup.copy(login = admin.login))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<ResponseEntity<ProblemDetail>>()
            .responseBodyContent!!
            .apply {
                map { it.toInt().toChar().toString() }
                    .reduce { request, s ->
                        request + buildString {
                            append(s)
                            if (s == VIRGULE && request.last().isDigit()) append("\n\t")
                        }
                    }.replace("{\"", "\n{\n\t\"")
                    .replace("\"}", "\"\n}")
                    .replace("\",\"", "\",\n\t\"")
                    .contains("Email is already in use!")
            }.logBody()
            .isNotEmpty()
            .run(::assertTrue)
    }


    @Test
    fun `test signup with an existing login`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            context.getBean<UserService>().signupService(signup)
            assertEquals(this@counts.first + 1, context.countUsers())
            assertEquals(this@counts.second + 1, context.countUserAuthority())
            assertEquals(third + 1, context.countUserActivation())
        }
        client
            .post()
            .uri(API_SIGNUP_PATH)
            .contentType(APPLICATION_PROBLEM_JSON)
            .bodyValue(signup.copy(email = "foo@localhost"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<ResponseEntity<ProblemDetail>>()
            .responseBodyContent!!
            .apply {
                map { it.toInt().toChar().toString() }
                    .reduce { request, s ->
                        request + buildString {
                            append(s)
                            if (s == VIRGULE && request.last().isDigit()) append("\n\t")
                        }
                    }.replace("{\"", "\n{\n\t\"")
                    .replace("\"}", "\"\n}")
                    .replace("\",\"", "\",\n\t\"")
                    .contains("Login name already used!")
            }.logBody()
            .isNotEmpty()
            .run(::assertTrue)
    }

    @Test
    fun `test signupService signup saves user and role_user and user_activation`(): Unit = runBlocking {
        Signup(
            login = "jdoe",
            email = "jdoe@acme.com",
            password = "secr3t",
            repassword = "secr3t"
        ).run signup@{
            Triple(
                context.countUsers(),
                context.countUserAuthority(),
                context.countUserActivation()
            ).run {
                assertEquals(0, first)
                assertEquals(0, second)
                assertEquals(0, third)
                context.getBean<UserService>().signupService(this@signup)
                assertEquals(first + 1, context.countUsers())
                assertEquals(second + 1, context.countUserAuthority())
                assertEquals(third + 1, context.countUserActivation())
            }
        }
    }

    @Test
    fun `Verifies the internationalization of validations by validator factory with a bad login in Italian`(): Unit {
        byProvider(HibernateValidator::class.java)
            .configure()
            .defaultLocale(ENGLISH)
            .locales(FRANCE, ITALY, US)
            .localeResolver {
                // get the locales supported by the client from the Accept-Language header
                val acceptLanguageHeader = "it-IT;q=0.9,en-US;q=0.7"
                val acceptedLanguages = LanguageRange.parse(acceptLanguageHeader)
                val resolvedLocales = filter(acceptedLanguages, it.supportedLocales)
                if (resolvedLocales.size > 0) resolvedLocales[0]
                else it.defaultLocale
            }
            .buildValidatorFactory()
            .validator
            .validateProperty(signup.copy(login = "funky-log(n"), LOGIN_FIELD)
            .run viol@{
                assertTrue(isNotEmpty())
                first().run {
                    assertEquals(
                        "{${Pattern::class.java.name}.message}",
                        messageTemplate
                    )
                    assertEquals(false, message.contains("doit correspondre à"))
                    assertContains(
                        "deve corrispondere a \"^(?>[a-zA-Z0-9!\$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)\$\"",
                        message
                    )
                }
            }
    }

    @Test
    fun `Verifies the internationalization of validations through REST with a non-conforming password in French`(): Unit =
        runBlocking {
            assertEquals(0, context.countUsers())
            client
                .post()
                .uri(API_SIGNUP_PATH)
                .contentType(APPLICATION_PROBLEM_JSON)
                .header(ACCEPT_LANGUAGE, FRENCH.language)
                .bodyValue(signup.copy(password = "123"))
                .exchange()
                .expectStatus()
                .isBadRequest
                .returnResult<ResponseEntity<ProblemDetail>>()
                .responseBodyContent!!
                .run {
                    assertTrue(isNotEmpty())
                    assertContains(responseToString(), "la taille doit")
                }
            assertEquals(0, context.countUsers())
        }

    @Test
    fun `test create userActivation inside signup`(): Unit = runBlocking {
        context.tripleCounts().run {
            (user to context).signupDao().apply {
                assertTrue(isRight())
                assertFalse(isLeft())
            }
            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())
        }
    }

    @Test
    fun `test find userActivation by key`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            (user to context).signupDao()
                .getOrNull()!!
                .run {
                    assertEquals(this@counts.first + 1, context.countUsers())
                    assertEquals(this@counts.second + 1, context.countUserAuthority())
                    assertEquals(third + 1, context.countUserActivation())
                    second.apply(::i)
                        .isBlank()
                        .run(::assertFalse)
                    assertEquals(
                        first,
                        context.findUserActivationByKey(second).getOrNull()!!.id
                    )
                    context.findUserActivationByKey(second).getOrNull().toString().run(::i)
                    // BabyStepping to find an implementation and debugging
                    assertDoesNotThrow {
                        first.toString().run(::i)
                        second.run(::i)
                        context.getBean<TransactionalOperator>().executeAndAwait {
                            FIND_BY_ACTIVATION_KEY
                                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                .bind(ACTIVATION_KEY_ATTR, second)
                                .fetch()
                                .awaitSingle()
                                .apply(::assertNotNull)
                                .apply { toString().run(::i) }
                                .let {
                                    UserActivation(
                                        id = UserActivation.Fields.ID_FIELD
                                            .run(it::get)
                                            .toString()
                                            .run(UUID::fromString),
                                        activationKey = ACTIVATION_KEY_FIELD
                                            .run(it::get)
                                            .toString(),
                                        createdDate = CREATED_DATE_FIELD
                                            .run(it::get)
                                            .toString()
                                            .run(LocalDateTime::parse)
                                            .toInstant(UTC),
                                        activationDate = ACTIVATION_DATE_FIELD
                                            .run(it::get)
                                            .run {
                                                when {
                                                    this == null || toString().lowercase() == "null" -> null
                                                    else -> toString().run(LocalDateTime::parse).toInstant(UTC)
                                                }
                                            },
                                    )
                                }.toString().run(::i)
                        }
                    }
                }
        }
    }

    @Test
    fun `test activate user by key`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            (user to context).signupDao().getOrNull()!!.run {
                assertEquals(
                    "null",
                    FIND_ALL_USERACTIVATION
                        .trimIndent()
                        .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                        .fetch()
                        .awaitSingleOrNull()!![ACTIVATION_DATE_FIELD]
                        .toString()
                        .lowercase()
                )
                assertEquals(this@counts.first + 1, context.countUsers())
                assertEquals(this@counts.second + 1, context.countUserAuthority())
                assertEquals(third + 1, context.countUserActivation())
                "user.id : $first".run(::i)
                "activation key : $second".run(::i)
                assertEquals(
                    1,
                    context.activateDao(second).getOrNull()!!
                )
                assertEquals(this@counts.first + 1, context.countUsers())
                assertEquals(this@counts.second + 1, context.countUserAuthority())
                assertEquals(third + 1, context.countUserActivation())
                assertNotEquals(
                    "null",
                    FIND_ALL_USERACTIVATION
                        .trimIndent()
                        .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                        .fetch()
                        .awaitSingleOrNull()!!
                        .apply { "user_activation : $this".run(::i) }[ACTIVATION_DATE_FIELD]
                        .toString()
                        .lowercase()
                )
            }
        }
    }

    @Test
    fun `test activate with key out of bound`(): Unit = runBlocking {
        UserActivation(
            id = randomUUID(),
            activationKey = random(
                ACTIVATION_KEY_SIZE * 2,
                0,
                0,
                true,
                true,
                null,
                SecureRandom().apply { 64.run(::ByteArray).run(::nextBytes) }
            )).run {
            "UserActivation : ${toString()}".run(::i)
            assertTrue(activationKey.length > ACTIVATION_KEY_SIZE)
            validate(mock<ServerWebExchange>()).run {
                assertTrue(isNotEmpty())
                assertTrue(size == 1)
                first().run {
                    assertTrue(keys.contains("objectName"))
                    assertTrue(values.contains(UserActivation.objectName))
                    assertTrue(keys.contains("field"))
                    assertTrue(values.contains(ACTIVATION_KEY_ATTR))
                    assertTrue(keys.contains("message"))
                    assertTrue(values.contains("size must be between 0 and 20"))
                }
            }
            context.activateDao(activationKey).run {
                isRight().run(::assertTrue)
                assertEquals(0, getOrNull()!!)
            }
            assertThrows<IllegalArgumentException>("Activation failed: No user was activated for key: $activationKey") {
                context.getBean<UserService>().activateService(activationKey)
            }
            context.getBean<UserService>().activateRequest(
                activationKey,
                mock<ServerWebExchange>()
            ).toString().run(::i)
        }
    }


    @Test
    fun `test activateService with a valid key`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            (user to context).signupDao().getOrNull()!!.run {
                assertEquals(
                    "null",
                    FIND_ALL_USERACTIVATION
                        .trimIndent()
                        .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                        .fetch()
                        .awaitSingleOrNull()!![ACTIVATION_DATE_FIELD]
                        .toString()
                        .lowercase()
                )
                assertEquals(this@counts.first + 1, context.countUsers())
                assertEquals(this@counts.second + 1, context.countUserAuthority())
                assertEquals(third + 1, context.countUserActivation())
                "user.id : $first".run(::i)
                "activation key : $second".run(::i)
                assertEquals(
                    1,
                    context.getBean<UserService>().activateService(second)
                )
                assertEquals(this@counts.first + 1, context.countUsers())
                assertEquals(this@counts.second + 1, context.countUserAuthority())
                assertEquals(third + 1, context.countUserActivation())
                assertNotEquals(
                    "null",
                    FIND_ALL_USERACTIVATION
                        .trimIndent()
                        .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                        .fetch()
                        .awaitSingleOrNull()!!
                        .apply { "user_activation : $this".run(::i) }[ACTIVATION_DATE_FIELD]
                        .toString()
                        .lowercase()
                )
            }
        }
    }


    @Test
    fun `test activate request with a wrong key producing a 412 PRECONDITION_FAILED`(): Unit {
        //user does not exist
        //user_activation does not exist
        //TODO: is wrong valid key?
        (API_ACTIVATE_PATH + API_ACTIVATE_PARAM to "wrongActivationKey").run UrlKeyPair@{
            client.get()
                .uri(first, second)
                .exchange()
                .expectStatus()
                .is4xxClientError
                .returnResult<ResponseEntity<ProblemDetail>>()
                .responseBodyContent!!.apply {
                    isNotEmpty().apply(::assertTrue)
                    map { it.toInt().toChar().toString() }
                        .reduce { request, s ->
                            request + buildString {
                                append(s)
                                if (s == VIRGULE && request.last().isDigit()) append("\n\t")
                            }
                        }.replace("{\"", "\n{\n\t\"")
                        .replace("\"}", "\"\n}")
                        .replace("\",\"", "\",\n\t\"")
                        .contains("Activation failed: No user was activated for key: $second")
                        .run(::assertTrue)
                }.logBody()
        }
    }

    @Test
    fun `test activate request with a valid key`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            (user to context).signupDao().getOrNull()!!.run {
                assertEquals(
                    "null",
                    FIND_ALL_USERACTIVATION
                        .trimIndent()
                        .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                        .fetch()
                        .awaitSingleOrNull()!![ACTIVATION_DATE_FIELD]
                        .toString()
                        .lowercase()
                )
                assertEquals(this@counts.first + 1, context.countUsers())
                assertEquals(this@counts.second + 1, context.countUserAuthority())
                assertEquals(third + 1, context.countUserActivation())
                "user.id : $first".run(::i)
                "activation key : $second".run(::i)

                context.findUserActivationByKey(second)
                    .getOrNull()!!
                    .activationDate
                    .run(::assertNull)

                client.get().uri(
                    API_ACTIVATE_PATH + API_ACTIVATE_PARAM,
                    second
                ).exchange()
                    .expectStatus()
                    .isOk
                    .returnResult<ResponseEntity<ProblemDetail>>()
                    .responseBodyContent!!
                    .logBody()

                context.findUserActivationByKey(second)
                    .getOrNull()!!
                    .activationDate
                    .run(::assertNotNull)
            }
        }
    }

    @Test
    fun `Verify the internationalization of validations by validator factory with a bad login in Italian`() {
        byProvider(HibernateValidator::class.java)
            .configure()
            .defaultLocale(ENGLISH)
            .locales(FRANCE, ITALY, US)
            .localeResolver {
                // get the locales supported by the client from the Accept-Language header
                val acceptLanguageHeader = "it-IT;q=0.9,en-US;q=0.7"
                val acceptedLanguages = LanguageRange.parse(acceptLanguageHeader)
                val resolvedLocales = filter(acceptedLanguages, it.supportedLocales)
                if (resolvedLocales.size > 0) resolvedLocales[0]
                else it.defaultLocale
            }
            .buildValidatorFactory()
            .validator
            .validateProperty(signup.copy(login = "funky-log(n"), LOGIN_FIELD)
            .run viol@{
                assertTrue(isNotEmpty())
                first().run {
                    assertEquals(
                        "{${Pattern::class.java.name}.message}",
                        messageTemplate
                    )
                    assertEquals(false, message.contains("doit correspondre à"))
                    assertContains(
                        "deve corrispondere a \"^(?>[a-zA-Z0-9!\$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)\$\"",
                        message
                    )
                }
            }
    }

    @Test
    fun `Verify the internationalization of validations through REST with a non-conforming password in French`() =
        runBlocking{
        assertEquals(0, context.countUsers())
        client
            .post()
            .uri(API_SIGNUP_PATH)
            .contentType(APPLICATION_PROBLEM_JSON)
            .header(ACCEPT_LANGUAGE, FRENCH.language)
            .bodyValue(signup.copy(password = "123"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<ResponseEntity<ProblemDetail>>()
            .responseBodyContent!!
            .run {
                assertTrue(isNotEmpty())
                assertContains(responseToString(), "la taille doit")
            }
        assertEquals(0, context.countUsers())

    }

    val gmailConfig = GoogleAuthConfig(
        clientId="729140334808-ql2f9rb3th81j15ct9uqnl4pjj61urt0.apps.googleusercontent.com",
        projectId = "gmail-tester-444502",
        authUri = "https://accounts.google.com/o/oauth2/auth",
        tokenUri = "https://oauth2.googleapis.com/token",
        authProviderX509CertUrl = "https://www.googleapis.com/oauth2/v1/certs",
        clientSecret = "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t",
        redirectUris = listOf("http://localhost:8080/oauth2/callback/google")
    )

//    private lateinit var mailService: MailService
//
//    private val properties: school.base.utils.Properties by lazy { context.getBean() }
//
//    private val messageSource: MessageSource by lazy { context.getBean() }
//
//    private val templateEngine: SpringWebFluxTemplateEngine by lazy { context.getBean() }
//
//    @Spy
//    lateinit var javaMailSender: JavaMailSenderImpl
//
//    @Captor
//    lateinit var messageCaptor: ArgumentCaptor<MimeMessage>
//
//    @BeforeEach
//    fun setup() {
//        openMocks(this)
//        doNothing()
//            .`when`(javaMailSender)
//            .send(any(MimeMessage::class.java))
//        mailService = MailServiceSmtp(
//            properties,
//            javaMailSender,
//            messageSource,
//            templateEngine
//        )
//    }
//
//
//    @Test
//    fun `test sendEmail`() {
//        mailService.sendEmail(
//            to = "john.doe@acme.com",
//            subject = "testSubject",
//            content = "testContent",
//            isMultipart = false,
//            isHtml = false
//        )
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        assertThat(message.subject).isEqualTo("testSubject")
//        assertThat(message.allRecipients[0]).hasToString("john.doe@acme.com")
//        assertThat(message.from[0]).hasToString(properties.mail.from)
//        assertThat(message.content).isInstanceOf(String::class.java)
//        assertThat(message.content).hasToString("testContent")
//        assertThat(message.dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
//    }
//
//    @Test
//    fun `test sendMail SendHtmlEmail`() {
//        mailService.sendEmail(
//            to = "john.doe@acme.com",
//            subject = "testSubject",
//            content = "testContent",
//            isMultipart = false,
//            isHtml = true
//        )
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        assertThat(message.subject).isEqualTo("testSubject")
//        assertThat("${message.allRecipients[0]}").isEqualTo("john.doe@acme.com")
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content).isInstanceOf(String::class.java)
//        assertThat(message.content.toString()).isEqualTo("testContent")
//        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
//    }
//
//    @Test
//    fun `test sendMail SendMultipartEmail`() {
//        mailService.sendEmail(
//            to = "john.doe@acme.com",
//            subject = "testSubject",
//            content = "testContent",
//            isMultipart = true,
//            isHtml = false
//        )
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        val part = ((message.content as MimeMultipart)
//            .getBodyPart(0).content as MimeMultipart)
//            .getBodyPart(0) as MimeBodyPart
//        val aos = ByteArrayOutputStream()
//        part.writeTo(aos)
//        assertThat(message.subject).isEqualTo("testSubject")
//        assertThat("${message.allRecipients[0]}").isEqualTo("john.doe@acme.com")
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content).isInstanceOf(Multipart::class.java)
//        assertThat("$aos").isEqualTo("\r\ntestContent")
//        assertThat(part.dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
//    }
//
//    @Test
//    fun `test sendMail SendMultipartHtmlEmail`() {
//        mailService.sendEmail(
//            to = "john.doe@acme.com",
//            subject = "testSubject",
//            content = "testContent",
//            isMultipart = true,
//            isHtml = true
//        )
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        val part = ((message.content as MimeMultipart)
//            .getBodyPart(0).content as MimeMultipart)
//            .getBodyPart(0) as MimeBodyPart
//        val aos = ByteArrayOutputStream()
//        part.writeTo(aos)
//        assertThat(message.subject).isEqualTo("testSubject")
//        assertThat("${message.allRecipients[0]}").isEqualTo("john.doe@acme.com")
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content).isInstanceOf(Multipart::class.java)
//        assertThat("$aos").isEqualTo("\r\ntestContent")
//        assertThat(part.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
//    }
//
//    @Test
//    fun `test SendEmailFromTemplate`() {
//        val user = AccountCredentials(
//            login = "john",
//            email = "john.doe@acme.com",
//            langKey = "en"
//        )
//        mailService.sendEmailFromTemplate(
//            user,
//            "mail/testEmail",
//            "email.test.title"
//        )
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        assertThat(message.subject).isEqualTo("test title")
//        assertThat("${message.allRecipients[0]}").isEqualTo(user.email)
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content.toString()).isEqualToNormalizingNewlines(
//            "<html>test title, http://127.0.0.1:8080, john</html>"
//        )
//        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
//    }
//
//    @Test
//    fun testSendActivationEmail() {
//        val user = AccountCredentials(
//            langKey = DEFAULT_LANGUAGE,
//            login = "john",
//            email = "john.doe@acme.com"
//        )
//        mailService.sendActivationEmail(user)
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        assertThat("${message.allRecipients[0]}").isEqualTo(user.email)
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content.toString()).isNotEmpty
//        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
//    }
//
//    @Test
//    fun testCreationEmail() {
//        val user = AccountCredentials(
//            langKey = DEFAULT_LANGUAGE,
//            login = "john",
//            email = "john.doe@acme.com",
//            resetKey = generateResetKey
//        )
//        mailService.sendCreationEmail(user)
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        assertThat("${message.allRecipients[0]}").isEqualTo(user.email)
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content.toString()).isNotEmpty
//        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
//    }
//
//    @Test
//    fun testSendPasswordResetMail() {
//        val user = AccountCredentials(
//            langKey = DEFAULT_LANGUAGE, login = "john", email = "john.doe@acme.com"
//        )
//        mailService.sendPasswordResetMail(user)
//        verify(javaMailSender).send(messageCaptor.capture())
//        val message = messageCaptor.value
//        assertThat("${message.allRecipients[0]}").isEqualTo(user.email)
//        assertThat("${message.from[0]}").isEqualTo(properties.mail.from)
//        assertThat(message.content.toString()).isNotEmpty
//        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
//    }
//
//    @Test
//    fun testSendEmailWithException() {
//        Mockito.doThrow(MailSendException::class.java).`when`(javaMailSender)
//            .send(any(MimeMessage::class.java))
//        try {
//            mailService.sendEmail(
//                "john.doe@acme.com", "testSubject", "testContent", isMultipart = false, isHtml = false
//            )
//        } catch (e: Exception) {
//            Assertions.fail<String>("Exception shouldn't have been thrown")
//        }
//    }
//
//    val languages = arrayOf(
//        "en", "fr", "de", "it", "es"
//    )
//    val PATTERN_LOCALE_3: Pattern = compile("([a-z]{2})-([a-zA-Z]{4})-([a-z]{2})")
//    val PATTERN_LOCALE_2: Pattern = compile("([a-z]{2})-([a-z]{2})")
//
//    @Test
//    fun testSendLocalizedEmailForAllSupportedLanguages() {
//        val user = AccountCredentials(
//            login = "john", email = "john.doe@acme.com"
//        )
//        for (langKey in languages) {
//            mailService.sendEmailFromTemplate(
//                user.copy(langKey = langKey), "mail/testEmail", "email.test.title"
//            )
//            verify(javaMailSender, atLeastOnce()).send(messageCaptor.capture())
//            val message = messageCaptor.value
//
//            val resource = this::class.java.classLoader.getResource(
//                "i18n/messages_${
//                    getJavaLocale(langKey)
//                }.properties"
//            )
//            assertNotNull(resource)
//            val prop = Properties()
//            prop.load(
//                InputStreamReader(
//                    FileInputStream(File(URI(resource.file).path)), Charset.forName("UTF-8")
//                )
//            )
//
//            val emailTitle = prop["email.test.title"] as String
//            assertThat(message.subject).isEqualTo(emailTitle)
//            assertThat(message.content.toString())
//                .isEqualToNormalizingNewlines("<html>$emailTitle, http://127.0.0.1:8080, john</html>")
//        }
//    }
//
//    private fun getJavaLocale(langKey: String): String {
//        var javaLangKey = langKey
//        val matcher2 = PATTERN_LOCALE_2.matcher(langKey)
//        if (matcher2.matches()) javaLangKey = "${
//            matcher2.group(1)
//        }_${
//            matcher2.group(2).uppercase()
//        }"
//        val matcher3 = PATTERN_LOCALE_3.matcher(langKey)
//        if (matcher3.matches()) javaLangKey = "${
//            matcher3.group(1)
//        }_${
//            matcher3.group(2)
//        }_${
//            matcher3.group(3).uppercase()
//        }"
//        return javaLangKey
//    }
//}
//
///*=================================================================================*/
//@Ignore
//class GmailServiceTests {
//
//    private lateinit var context: ConfigurableApplicationContext
//
//    @BeforeAll
//    fun `lance le server en profile test`() {
//        context = launcher(GMAIL)
//    }
//
//    @AfterAll
//    fun `arrête le serveur`() = context.close()
//}
//
///*=================================================================================*/
//
}

////import org.assertj.base.api.Assertions.assertThat
////import org.springframework.test.web.reactive.server.returnResult
////import java.net.URI
//import jakarta.validation.Validator
//import org.junit.jupiter.api.AfterAll
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeAll
//import org.springframework.beans.factory.getBean
//import org.springframework.context.ConfigurableApplicationContext
//import org.springframework.data.r2dbc.base.R2dbcEntityTemplate
//import org.springframework.test.web.reactive.server.WebTestClient
//import org.springframework.test.web.reactive.server.WebTestClient.bindToServer
//import school.base.property.BASE_URL_DEV
//import school.base.logging.i
//import school.deleteAllAccounts
//import school.launcher
//import kotlin.test.Test
//
//internal class PasswordControllerTests {
//
//    private lateinit var context: ConfigurableApplicationContext
//    private val dao: R2dbcEntityTemplate by lazy { context.getBean() }
//    private val validator: Validator by lazy { context.getBean() }
//    private val client: WebTestClient by lazy {
//        bindToServer()
//            .baseUrl(BASE_URL_DEV)
//            .build()
//    }
//
//    @BeforeAll
//    fun `lance le server en profile test`() {
//        context = launcher()
//    }
//
//    @AfterAll
//    fun `arrête le serveur`() = context.close()
//
//    @AfterEach
//    fun tearDown() = deleteAllAccounts(dao)
//    @Test
//    fun `test Change Password Wrong Existing Password`() {
//        i("test Change Password Wrong Existing Password")
//        //TODO: activate account
//        //TODO: get bearer by login
//    }
//    /*
//        @Test
//        @WithMockUser("change-password-wrong-existing-password")
//        fun testChangePasswordWrongExistingPassword() {
//            val currentPassword = RandomStringUtils.random(60)
//            val user = User(
//                password = passwordEncoder.encode(currentPassword),
//                login = "change-password-wrong-existing-password",
//                createdBy = SYSTEM_ACCOUNT,
//                email = "change-password-wrong-existing-password@example.com"
//            )
//
//            userRepository.save(user).block()
//
//            accountWebTestClient.post().uri("/api/account/change-password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO("1$currentPassword", "new password")))
//                .exchange()
//                .expectStatus().isBadRequest
//
//            val updatedUser = userRepository.findOneByLogin("change-password-wrong-existing-password").block()
//            assertThat(passwordEncoder.matches("new password", updatedUser.password)).isFalse
//            assertThat(passwordEncoder.matches(currentPassword, updatedUser.password)).isTrue
//        }
//
//        @Test
//        @WithMockUser("change-password")
//        fun testChangePassword() {
//            val currentPassword = RandomStringUtils.random(60)
//            val user = User(
//                password = passwordEncoder.encode(currentPassword),
//                login = "change-password",
//                createdBy = SYSTEM_ACCOUNT,
//                email = "change-password@example.com"
//            )
//
//            userRepository.save(user).block()
//
//            accountWebTestClient.post().uri("/api/account/change-password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, "new password")))
//                .exchange()
//                .expectStatus().isOk
//
//            val updatedUser = userRepository.findOneByLogin("change-password").block()
//            assertThat(passwordEncoder.matches("new password", updatedUser.password)).isTrue
//        }
//
//        @Test
//        @WithMockUser("change-password-too-small")
//        fun testChangePasswordTooSmall() {
//            val currentPassword = RandomStringUtils.random(60)
//            val user = User(
//                password = passwordEncoder.encode(currentPassword),
//                login = "change-password-too-small",
//                createdBy = SYSTEM_ACCOUNT,
//                email = "change-password-too-small@example.com"
//            )
//
//            userRepository.save(user).block()
//
//            val newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MIN_LENGTH - 1)
//
//            accountWebTestClient.post().uri("/api/account/change-password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, newPassword)))
//                .exchange()
//                .expectStatus().isBadRequest
//
//            val updatedUser = userRepository.findOneByLogin("change-password-too-small").block()
//            assertThat(updatedUser.password).isEqualTo(user.password)
//        }
//
//        @Test
//        @WithMockUser("change-password-too-long")
//        fun testChangePasswordTooLong() {
//            val currentPassword = RandomStringUtils.random(60)
//            val user = User(
//                password = passwordEncoder.encode(currentPassword),
//                login = "change-password-too-long",
//                createdBy = SYSTEM_ACCOUNT,
//                email = "change-password-too-long@example.com"
//            )
//
//            userRepository.save(user).block()
//
//            val newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MAX_LENGTH + 1)
//
//            accountWebTestClient.post().uri("/api/account/change-password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, newPassword)))
//                .exchange()
//                .expectStatus().isBadRequest
//
//            val updatedUser = userRepository.findOneByLogin("change-password-too-long").block()
//            assertThat(updatedUser.password).isEqualTo(user.password)
//        }
//
//        @Test
//        @WithMockUser("change-password-empty")
//        fun testChangePasswordEmpty() {
//            val currentPassword = RandomStringUtils.random(60)
//            val user = User(
//                password = passwordEncoder.encode(currentPassword),
//                login = "change-password-empty",
//                createdBy = SYSTEM_ACCOUNT,
//                email = "change-password-empty@example.com"
//            )
//
//            userRepository.save(user).block()
//
//            accountWebTestClient.post().uri("/api/account/change-password")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, "")))
//                .exchange()
//                .expectStatus().isBadRequest
//
//            val updatedUser = userRepository.findOneByLogin("change-password-empty").block()
//            assertThat(updatedUser.password).isEqualTo(user.password)
//        }
//     */
//    @Test
//    fun `test Request Password Reset`() {
//    }
////    val user = User(
////        password = RandomStringUtils.random(60),
////        activated = true,
////        login = "password-reset",
////        createdBy = SYSTEM_ACCOUNT,
////        email = "password-reset@example.com"
////    )
////
////    userRepository.save(user).block()
////
////    accountWebTestClient.post().uri("/api/account/reset-password/init")
////        .bodyValue("password-reset@example.com")
////        .exchange()
////        .expectStatus().isOk
//
//
//    @Test
//    fun `test Request Password Reset UpperCaseEmail`() {
//    }
////    val user = User(
////        password = RandomStringUtils.random(60),
////        activated = true,
////        login = "password-reset-upper-case",
////        createdBy = SYSTEM_ACCOUNT,
////        email = "password-reset-upper-case@example.com"
////    )
////
////    userRepository.save(user).block()
////
////    accountWebTestClient.post().uri("/api/account/reset-password/init")
////    .bodyValue("password-reset-upper-case@EXAMPLE.COM")
////    .exchange()
////    .expectStatus().isOk
//
//    @Test
//    fun `test Request Password Reset Wrong Email`() {
////        accountWebTestClient.post().uri("/api/account/reset-password/init")
////            .bodyValue("password-reset-wrong-email@example.com")
////            .exchange()
////            .expectStatus().isOk
//    }
//
//    @Test
////    @Throws(Exception::class)
//    fun `test Finish Password Reset`() {
////        val user = User(
////            password = RandomStringUtils.random(60),
////            login = "finish-password-reset",
////            email = "finish-password-reset@example.com",
////            resetDate = Instant.now().plusSeconds(60),
////            createdBy = SYSTEM_ACCOUNT,
////            resetKey = "reset key"
////        )
////
////        userRepository.save(user).block()
////
////        val keyAndPassword = KeyAndPasswordVM(key = user.resetKey, newPassword = "new password")
////
////        accountWebTestClient.post().uri("/api/account/reset-password/finish")
////            .contentType(MediaType.APPLICATION_JSON)
////            .bodyValue(convertObjectToJsonBytes(keyAndPassword))
////            .exchange()
////            .expectStatus().isOk
////
////        val updatedUser = userRepository.findOneByLogin(user.login!!).block()
////        assertThat(passwordEncoder.matches(keyAndPassword.newPassword, updatedUser.password)).isTrue
//    }
//
//    @Test
////    @Throws(Exception::class)
//    fun `test Finish Password Reset Too Small`() {
////        val user = User(
////            password = RandomStringUtils.random(60),
////            login = "finish-password-reset-too-small",
////            email = "finish-password-reset-too-small@example.com",
////            resetDate = Instant.now().plusSeconds(60),
////            createdBy = SYSTEM_ACCOUNT,
////            resetKey = "reset key too small"
////        )
////
////        userRepository.save(user).block()
////
////        val keyAndPassword = KeyAndPasswordVM(key = user.resetKey, newPassword = "foo")
////
////        accountWebTestClient.post().uri("/api/account/reset-password/finish")
////            .contentType(MediaType.APPLICATION_JSON)
////            .bodyValue(convertObjectToJsonBytes(keyAndPassword))
////            .exchange()
////            .expectStatus().isBadRequest
////
////        val updatedUser = userRepository.findOneByLogin(user.login!!).block()
////        assertThat(passwordEncoder.matches(keyAndPassword.newPassword, updatedUser.password)).isFalse
//    }
//
//    @Test
////    @Throws(Exception::class)
//    fun `test Finish Password Reset Wrong Key`() {
////        val keyAndPassword = KeyAndPasswordVM(key = "wrong reset key", newPassword = "new password")
////
////        accountWebTestClient.post().uri("/api/account/reset-password/finish")
////            .contentType(MediaType.APPLICATION_JSON)
////            .bodyValue(convertObjectToJsonBytes(keyAndPassword))
////            .exchange()
////            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
//    }
//}

