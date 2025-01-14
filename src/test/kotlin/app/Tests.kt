@file:Suppress(
    "NonAsciiCharacters",
    "SqlResolve",
    "RedundantUnitReturnType",
    "unused"
)

package app

import app.TestUtils.Data.DEFAULT_USER_JSON
import app.TestUtils.Data.OFFICIAL_SITE
import app.TestUtils.Data.admin
import app.TestUtils.Data.signup
import app.TestUtils.Data.user
import app.TestUtils.Data.users
import app.TestUtils.FIND_ALL_USERACTIVATION
import app.TestUtils.FIND_BY_ACTIVATION_KEY
import app.TestUtils.FIND_USER_BY_LOGIN
import app.TestUtils.countUserActivation
import app.TestUtils.countUserAuthority
import app.TestUtils.countUsers
import app.TestUtils.defaultRoles
import app.TestUtils.delete
import app.TestUtils.deleteAllUsersOnly
import app.TestUtils.findAuthsByEmail
import app.TestUtils.findAuthsByLogin
import app.TestUtils.findOne
import app.TestUtils.findUserActivationByKey
import app.TestUtils.findUserById
import app.TestUtils.logBody
import app.TestUtils.responseToString
import app.TestUtils.tripleCounts
import app.core.Constants.DEFAULT_LANGUAGE
import app.core.Constants.DEVELOPMENT
import app.core.Constants.EMPTY_STRING
import app.core.Constants.PATTERN_LOCALE_2
import app.core.Constants.PATTERN_LOCALE_3
import app.core.Constants.PRODUCTION
import app.core.Constants.ROLE_USER
import app.core.Constants.STARTUP_LOG_MSG_KEY
import app.core.Constants.USER
import app.core.Constants.VIRGULE
import app.core.Constants.languages
import app.core.Loggers.i
import app.core.Properties
import app.core.Utils.lsWorkingDir
import app.core.Utils.lsWorkingDirProcess
import app.core.Utils.toJson
import app.core.database.EntityModel.Companion.MODEL_FIELD_FIELD
import app.core.database.EntityModel.Companion.MODEL_FIELD_MESSAGE
import app.core.database.EntityModel.Companion.MODEL_FIELD_OBJECTNAME
import app.core.database.EntityModel.Members.withId
import app.core.mail.MailConfiguration.GoogleAuthConfig
import app.core.security.SecurityUtils.generateActivationKey
import app.core.security.SecurityUtils.generateResetKey
import app.core.security.SecurityUtils.getCurrentUserLogin
import app.core.web.HttpUtils.validator
import app.users.User
import app.users.User.Attributes.EMAIL_ATTR
import app.users.User.Attributes.LOGIN_ATTR
import app.users.User.Attributes.PASSWORD_ATTR
import app.users.User.Relations.FIND_ALL_USERS
import app.users.User.Relations.Fields.ID_FIELD
import app.users.User.Relations.Fields.LOGIN_FIELD
import app.users.User.Relations.Fields.PASSWORD_FIELD
import app.users.UserDao.availability
import app.users.UserDao.change
import app.users.UserDao.findOne
import app.users.UserDao.save
import app.users.UserDao.signup
import app.users.mail.MailService
import app.users.mail.MailServiceSmtp
import app.users.password.InvalidPasswordException
import app.users.password.PasswordChange
import app.users.password.PasswordChange.Attributes.CURRENT_PASSWORD_ATTR
import app.users.password.PasswordEndPoint.API_CHANGE_PASSWORD_PATH
import app.users.password.PasswordService
import app.users.security.Role
import app.users.security.RoleDao.countRoles
import app.users.security.UserRole
import app.users.signup.Signup
import app.users.signup.Signup.Companion.objectName
import app.users.signup.Signup.Constraints.PASSWORD_MAX
import app.users.signup.Signup.Constraints.PASSWORD_MIN
import app.users.signup.SignupEndPoint.API_ACTIVATE_PARAM
import app.users.signup.SignupEndPoint.API_ACTIVATE_PATH
import app.users.signup.SignupEndPoint.API_SIGNUP_PATH
import app.users.signup.SignupService
import app.users.signup.SignupService.Companion.ONE_ROW_UPDATED
import app.users.signup.SignupService.Companion.SIGNUP_AVAILABLE
import app.users.signup.SignupService.Companion.SIGNUP_EMAIL_NOT_AVAILABLE
import app.users.signup.SignupService.Companion.SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE
import app.users.signup.SignupService.Companion.SIGNUP_LOGIN_NOT_AVAILABLE
import app.users.signup.UserActivation
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Companion.ACTIVATION_KEY_SIZE
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_DATE_FIELD
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_KEY_FIELD
import app.users.signup.UserActivation.Relations.Fields.CREATED_DATE_FIELD
import app.users.signup.UserActivationDao.activate
import app.users.signup.UserActivationDao.validate
import app.workspace.Installer
import app.workspace.Workspace
import app.workspace.Workspace.Companion.install
import app.workspace.Workspace.InstallationType.ALL_IN_ONE
import app.workspace.Workspace.InstallationType.SEPARATED_FOLDERS
import app.workspace.Workspace.WorkspaceConfig
import app.workspace.Workspace.WorkspaceEntry
import app.workspace.Workspace.WorkspaceEntry.CollaborationEntry.Collaboration
import app.workspace.Workspace.WorkspaceEntry.CommunicationEntry.Communication
import app.workspace.Workspace.WorkspaceEntry.ConfigurationEntry.Configuration
import app.workspace.Workspace.WorkspaceEntry.CoreEntry.Education
import app.workspace.Workspace.WorkspaceEntry.CoreEntry.Education.EducationEntry.*
import app.workspace.Workspace.WorkspaceEntry.DashboardEntry.Dashboard
import app.workspace.Workspace.WorkspaceEntry.JobEntry.Job
import app.workspace.Workspace.WorkspaceEntry.JobEntry.Job.HumanResourcesEntry.Position
import app.workspace.Workspace.WorkspaceEntry.JobEntry.Job.HumanResourcesEntry.Resume
import app.workspace.Workspace.WorkspaceEntry.OfficeEntry.Office
import app.workspace.Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.*
import app.workspace.Workspace.WorkspaceEntry.OrganisationEntry.Organisation
import app.workspace.Workspace.WorkspaceEntry.PortfolioEntry.Portfolio
import app.workspace.Workspace.WorkspaceEntry.PortfolioEntry.Portfolio.PortfolioProject
import app.workspace.Workspace.WorkspaceEntry.PortfolioEntry.Portfolio.PortfolioProject.ProjectBuild
import app.workspace.WorkspaceManager
import app.workspace.WorkspaceManager.WorkspaceConstants.entries
import app.workspace.WorkspaceManager.displayWorkspaceStructure
import app.workspace.WorkspaceManager.workspace
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.mail.Multipart
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.validation.Validation.byProvider
import jakarta.validation.Validator
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils.random
import org.apache.commons.lang3.SystemUtils.USER_HOME_KEY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.swing.edt.GuiActionRunner.execute
import org.assertj.swing.fixture.FrameFixture
import org.hibernate.validator.HibernateValidator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mockito.doThrow
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.Spy
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.core.env.get
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.web.server.ServerWebExchange
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.*
import java.util.Locale.*
import java.util.UUID.fromString
import java.util.UUID.randomUUID
import javax.inject.Inject
import kotlin.io.path.pathString
import kotlin.test.*


@ActiveProfiles("test")
@TestInstance(PER_CLASS)
@SpringBootTest(
    classes = [API::class],
    properties = ["spring.main.web-application-type=reactive"],
)
class Tests {

    @Inject
    lateinit var context: ApplicationContext
    lateinit var client: WebTestClient
    lateinit var mailService: MailService

    @Spy
    lateinit var javaMailSender: JavaMailSenderImpl

    @Captor
    lateinit var messageCaptor: ArgumentCaptor<MimeMessage>


    val gmailConfig by lazy {
        GoogleAuthConfig(
            clientId = "729140334808-ql2f9rb3th81j15ct9uqnl4pjj61urt0.apps.googleusercontent.com",
            projectId = "gmail-tester-444502",
            authUri = "https://accounts.google.com/o/oauth2/auth",
            tokenUri = "https://oauth2.googleapis.com/token",
            authProviderX509CertUrl = "https://www.googleapis.com/oauth2/v1/certs",
            clientSecret = "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t",
            redirectUris = listOf("http://localhost:${context.environment["server.port"]}/oauth2/callback/google")
        )
    }

    @BeforeTest
    fun setUp(context: ApplicationContext) {
        client = context.run(WebTestClient::bindToApplicationContext).build()
        openMocks(this)
        doNothing()
            .`when`(javaMailSender)
            .send(any(MimeMessage::class.java))
        mailService = MailServiceSmtp(
            context.getBean<Properties>(),
            javaMailSender,
            context.getBean<MessageSource>(),
            context.getBean<SpringWebFluxTemplateEngine>()
        )
    }

    @AfterTest
    fun cleanUp(context: ApplicationContext): Unit = runBlocking { context.deleteAllUsersOnly() }

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
            assertThat("Cher $this").isEqualTo(
                context.getBean<MessageSource>().getMessage(
                    "email.activation.greeting",
                    arrayOf(this),
                    FRENCH
                )
            )
        }


    @Test
    fun `ConfigurationsTests - MessageSource test message startupLog`(): Unit {
        assertThat(buildString {
            append("You have misconfigured your application!\n")
            append("It should not run with both the $DEVELOPMENT\n")
            append("and $PRODUCTION profiles at the same time.")
        }).isEqualTo(
            context.getBean<MessageSource>().getMessage(
                STARTUP_LOG_MSG_KEY,
                arrayOf(DEVELOPMENT, PRODUCTION),
                getDefault()
            ).apply { i(this) })
    }

    @Test
    fun `ConfigurationsTests - test go visit message`(): Unit {
        assertThat(OFFICIAL_SITE).isEqualTo(context.getBean<Properties>().goVisitMessage)
    }

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
    fun `test signup and trying to retrieve the user id from databaseClient object`(): Unit =
        runBlocking {
            assertThat(context.countUsers()).isEqualTo(0)
            (user to context).signup().onRight {
                //Because 36 == UUID.toString().length
                it.toString()
                    .apply { assertThat(it.first.toString().length).isEqualTo(36) }
                    .apply(::i)
            }
            assertThat(context.countUsers()).isEqualTo(1)
            assertDoesNotThrow {
                FIND_ALL_USERS
                    .trimIndent()
                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                    .fetch()
                    .all()
                    .collect {
                        it[ID_FIELD]
                            .toString()
                            .run(UUID::fromString)
                    }
            }
        }

    @Test
    fun `test findOneWithAuths using one query`(): Unit = runBlocking {
        context.tripleCounts().run {
            assertEquals(Triple(0, 0, 0), this)
            (user to context).signup()
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
                    id = fromString(get(ID_FIELD).toString()),
                    email = get(User.Relations.Fields.EMAIL_FIELD).toString(),
                    login = get(LOGIN_FIELD).toString(),
                    roles = get(User.Members.ROLES_MEMBER)
                        .toString()
                        .split(",")
                        .map { Role(it) }
                        .toSet(),
                    password = get(PASSWORD_FIELD).toString(),
                    langKey = get(User.Relations.Fields.LANG_KEY_FIELD).toString(),
                    version = get(User.Relations.Fields.VERSION_FIELD).toString().toLong(),
                )
                val userResult = context
                    .findOne<User>(user.login)
                    .getOrNull()!!
                assertNotNull(expectedUserResult)
                assertNotNull(expectedUserResult.id)
                assertEquals(expectedUserResult.roles.first().id, ROLE_USER)
                assertEquals(1, expectedUserResult.roles.size)
                assertEquals(expectedUserResult.id, userResult.id)
                assertEquals(expectedUserResult.email, userResult.email)
                assertEquals(expectedUserResult.login, userResult.login)
                assertEquals(expectedUserResult.langKey, userResult.langKey)
                assertEquals(expectedUserResult.version, userResult.version)
                assertTrue {
                    expectedUserResult.roles.isNotEmpty()
                    context.getBean<PasswordEncoder>()
                        .matches(user.password, expectedUserResult.password)
                    context.getBean<PasswordEncoder>().matches(user.password, userResult.password)
                }
                assertEquals(expectedUserResult.roles.first().id, ROLE_USER)
                assertEquals(userResult.roles.first().id, ROLE_USER)
                assertEquals(userResult.roles.size, 1)

            }
    }

    @Test
    fun `test findOneWithAuths`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        assertEquals(0, context.countUserAuthority())
        val userId: UUID = (user to context).signup().getOrNull()!!.first
        userId.apply { run(::assertNotNull) }
            .run { "(user to context).signup() : $this" }
            .run(::println)
        assertEquals(1, context.countUsers())
        assertEquals(1, context.countUserAuthority())
        context.findOne<User>(user.email)
            .getOrNull()
            ?.apply {
                run(::assertNotNull)
                assertEquals(1, roles.size)
                assertEquals(ROLE_USER, roles.first().id)
                assertEquals(userId, id)
            }.run { "context.findOneWithAuths<User>(${user.email}).getOrNull() : $this" }
            .run(::i)
        context.findOne<User>(userId).getOrNull()
            .run { "context.findOneDraft<User>(user.email).getOrNull() : $this" }
            .run(::i)
        context.findAuthsByEmail(user.email).getOrNull()
            .run { "context.findAuthsByEmail(${user.email}).getOrNull() : $this" }
            .run(::i)
    }

    @Test
    fun `test findOne`(): Unit = runBlocking {
        assertEquals(0, context.countUsers())
        (user to context).save()
        assertEquals(1, context.countUsers())
        val findOneEmailResult: Either<Throwable, User> = context.findOne<User>(user.email)
        findOneEmailResult.map { assertDoesNotThrow { fromString(it.id.toString()) } }
        i("findOneEmailResult : ${findOneEmailResult.getOrNull()}")
        context.findOne<User>(user.login)
            .map { assertDoesNotThrow { fromString(it.id.toString()) } }
    }

    @Test
    fun `test findUserById`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        lateinit var userWithAuths: User
        (user to context).signup().apply {
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
        "userWithAuths : $userWithAuths".run(::i)
        "userResult : $userResult".run(::i)
    }

    @Test
    fun `test findAuthsByLogin`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        lateinit var userWithAuths: User
        (user to context).signup().apply {
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
        "userWithAuths : $userWithAuths".run(::i)
    }

    @Test
    fun `test findAuthsByEmail`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        lateinit var userWithAuths: User
        (user to context).signup().apply {
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
        "userWithAuths : $userWithAuths".run(::i)
    }

    @Test
    fun `test findOneWithAuths with existing email login and roles`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertEquals(0, countUserBefore)
        val countUserAuthBefore = context.countUserAuthority()
        assertEquals(0, countUserAuthBefore)
        (user to context).signup()
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
            (user to context).signup()
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
                    assertEquals(rows[Role.Relations.Fields.ID_FIELD], ROLE_USER)
                    resultRoles.add(rows[Role.Relations.Fields.ID_FIELD].toString())
                }
            assertEquals(ROLE_USER, resultRoles.first())
            assertEquals(ROLE_USER, resultRoles.first())
            assertEquals(1, context.countUsers())
            assertEquals(1, context.countUserAuthority())
        }

    @Test
    fun `try to do implementation of findOneWithAuths with existing email login and roles`(): Unit =
        runBlocking {
            val countUserBefore = context.countUsers()
            assertEquals(0, countUserBefore)
            val countUserAuthBefore = context.countUserAuthority()
            assertEquals(0, countUserAuthBefore)
            val resultRoles = mutableSetOf<Role>()
            lateinit var resultUserId: UUID
            (user to context).signup().apply {
                assertTrue(isRight())
                assertFalse(isLeft())
            }.onRight {
                """
            SELECT ur."role" 
            FROM user_authority AS ur 
            WHERE ur.user_id = :userId"""
                    .trimIndent()
                    .run(context.getBean<DatabaseClient>()::sql)
                    .bind(UserRole.Attributes.USER_ID_ATTR, it.first)
                    .fetch()
                    .all()
                    .collect { rows ->
                        assertEquals(rows[Role.Relations.Fields.ID_FIELD], ROLE_USER)
                        resultRoles.add(Role(id = rows[Role.Relations.Fields.ID_FIELD].toString()))
                    }
                assertEquals(
                    ROLE_USER,
                    user.withId(it.first).copy(
                        roles =
                        resultRoles
                            .map { it.id.run(::Role) }
                            .toMutableSet())
                        .roles.first().id
                )
                resultUserId = it.first
            }
            assertThat(resultUserId.toString().length).isEqualTo(randomUUID().toString().length)
            assertDoesNotThrow { fromString(resultUserId.toString()) }
            assertThat(ROLE_USER).isEqualTo(resultRoles.first().id)
            assertThat(context.countUsers()).isEqualTo(1)
            assertThat(context.countUserAuthority()).isEqualTo(1)
        }


    @Test//TODO: rewrite test showing the scenario clearly
    fun `test UserRoleDao signup with existing user without user_role`(): Unit = runBlocking {
        val countUserBefore = context.countUsers()
        assertThat(countUserBefore).isEqualTo(0)
        val countUserAuthBefore = context.countUserAuthority()
        assertThat(countUserAuthBefore).isEqualTo(0)
        lateinit var result: Either<Throwable, UUID>
        (user to context).save()
        assertThat(context.countUsers()).isEqualTo(countUserBefore + 1)
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
        SELECT ua.${UserRole.Relations.Fields.ID_FIELD} 
        FROM ${UserRole.Relations.Fields.TABLE_NAME} AS ua 
        where ua.user_id= :userId and ua."role" = :role"""
            .trimIndent()
            .run(context.getBean<DatabaseClient>()::sql)
            .bind(UserRole.Attributes.USER_ID_ATTR, userId)
            .bind(UserRole.Attributes.ROLE_ATTR, ROLE_USER)
            .fetch()
            .one()
            .awaitSingle()[UserRole.Relations.Fields.ID_FIELD]
            .toString()
            .let { "user_role_id : $it" }
            .run(::i)
        assertThat(context.countUserAuthority()).isEqualTo(countUserAuthBefore + 1)
    }

    @Test
    fun `check findOneByEmail with non-existing email`(): Unit = runBlocking {
        assertEquals(
            0,
            context.countUsers(),
            "context should not have a user recorded in database"
        )
        context.findOne<User>("user@dummy.com").apply {
            assertFalse(isRight())
            assertTrue(isLeft())
        }.mapLeft { assertThat(it).isInstanceOf(Throwable::class.java) }
    }

    @Test
    fun `check findOne with existing email`(): Unit = runBlocking {
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

        context.findOne<User>(user.email).apply {
            assertTrue(isRight())
            assertFalse(isLeft())
        }.map { assertDoesNotThrow { fromString(it.id.toString()) } }
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
        users.forEach { (it to context).signup() }
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
        val ids = users.map { (it to context).signup().getOrNull()!! }
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
            ) to context).availability().run {
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
            (signup to context).availability().run {
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
            ) to context).availability().run {
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
            ) to context).availability().run {
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
                        PASSWORD_FIELD to password,
                        User.Relations.Fields.EMAIL_FIELD to email,
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
            .uri("$API_SIGNUP_PATH/foobar")
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
        context.findOne<User>(user.email).run {
            when (this) {
                is Left -> assertEquals(
                    Exception::class.java,
                    value::class.java
                )

                is Right -> assertEquals(user.id, value.id)
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
            context.getBean<SignupService>().signup(signup)
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
            context.getBean<SignupService>().signup(signup)
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
    fun `test signupService signup saves user and role_user and user_activation`(): Unit =
        runBlocking {
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
                    context.getBean<SignupService>().signup(this@signup)
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
                assertThat(isNotEmpty()).isTrue
                first().run {
                    assertEquals(
                        "{${Pattern::class.java.name}.message}",
                        messageTemplate
                    )
                    assertThat(message)
                        .contains("deve corrispondere a \"^(?>[a-zA-Z0-9!\$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)\$\"")
                        .doesNotContain("doit correspondre à")
                }
            }
    }

    @Test
    fun `Verifies the internationalization of validations through REST with an unconform password in French`(): Unit =
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
            (user to context).signup().apply { assertThat(isRight()).isTrue }
            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())
        }
    }

    @Test
    fun `test find userActivation by key`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            (user to context).signup()
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
                                        id = UserActivation.Relations.Fields.ID_FIELD
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
                                                    else -> toString().run(LocalDateTime::parse)
                                                        .toInstant(UTC)
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
            (user to context).signup().getOrNull()!!.run {
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
                    context.activate(second).getOrNull()!!
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
            i("UserActivation : ${toString()}")
            validate(mock<ServerWebExchange>()).run {
                assertTrue {
                    activationKey.length > ACTIVATION_KEY_SIZE
                    isNotEmpty()
                    size == 1
                }
                first().run {
                    assertTrue {
                        keys.contains("objectName")
                        values.contains(UserActivation.objectName)
                        keys.contains("field")
                        values.contains(ACTIVATION_KEY_ATTR)
                        keys.contains("message")
                        values.contains("size must be between 0 and 20")
                    }
                }
            }
            context.activate(activationKey).run {
                isRight().run(::assertTrue)
                onRight { assertThat(it).isEqualTo(0) }
            }
            assertThrows<IllegalArgumentException>("Activation failed: No user was activated for key: $activationKey") {
                context.getBean<SignupService>().activate(activationKey)
            }
            context.getBean<SignupService>().activate(
                activationKey,
                mock<ServerWebExchange>()
            ).toString().run(::i)
        }
    }


    @Test
    fun `test activateService with a valid key`(): Unit = runBlocking {
        context.tripleCounts().run counts@{
            (user to context).signup().getOrNull()!!.run {
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
                    context.getBean<SignupService>().activate(second)
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
            (user to context).signup().getOrNull()!!.run {
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
    fun `test dao update user password`(): Unit = runBlocking {
        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(user.password, second, "password should be different")
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(user.password, second),
                        message = "password should be encoded"
                    )

                    "*updatedPassword123".run {
                        (user.copy(id = first, password = this) to context)
                            .change()
                            .apply { assertFalse(isLeft()) }
                            .map {
                                i("row updated : $it")
                                assertEquals(ONE_ROW_UPDATED, it)
                            }
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                this, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { "password retrieved after user update: $it".run(::i) }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )
                    }
                }
        }
    }

    @Test
    @WithMockUser(username = USER, roles = [ROLE_USER])
    fun `test service update user password`(): Unit = runBlocking {
        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(user.password, second, "password should be different")
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(user.password, second),
                        message = "password should be encoded"
                    )

                    "*updatedPassword123".run {
                        assertEquals(user.login, getCurrentUserLogin())
                        assertEquals(
                            ONE_ROW_UPDATED,
                            context.getBean<PasswordService>().change(user.password, this)
                        )
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                this, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )
                    }
                }
        }
    }

    @Test
    @WithMockUser("change-password-wrong-existing-password")
    fun testChangePasswordWrongExistingPassword(): Unit = runBlocking {
        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(user.password, second, "password should be different")
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(user.password, second),
                        message = "password should be encoded"
                    )

                    "*updatedPassword123".run {
                        assertNotEquals(user.login, getCurrentUserLogin())
                        assertThrows<InvalidPasswordException> {
                            context.getBean<PasswordService>().change(user.password, this)
                        }
                    }
                }
        }
    }

    @Test
    @WithMockUser("change-password-wrong-existing-password")
    fun testControllerChangePasswordWrongExistingPassword(): Unit = runBlocking {
        val testLogin = "change-password-wrong-existing-password"
        val testPassword = "change-password-wrong-existing-password"
        assertThat(user.id).isNull()
        context.tripleCounts().run triple@{
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertThat(this@triple.first + 1).isEqualTo(context.countUsers())
            assertThat(this@triple.second + 1).isEqualTo(context.countUserActivation())
            assertThat(this@triple.third + 1).isEqualTo(context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch()
                .awaitSingle()
                .run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }
                .run pairUuidPassword@{
                    i("user.id retrieved before update password: ${this@pairUuidPassword.first}")
                    assertEquals(uuid, this@pairUuidPassword.first, "user.id should be the same")
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should be encoded and match"
                    )
                    "*updatedPassword123".run updatedPassword@{
                        assertThat(getCurrentUserLogin())
                            .isNotEqualTo(this@updatedPassword)
                            .isEqualTo(testLogin)
                        context.getBean<PasswordService>().change(
                            currentClearTextPassword = testPassword,
                            newPassword = this
                        )
                        context.findOne<User>(testLogin)
                            .apply { assertThat(isRight()).isTrue }
                            .getOrNull()!!.run {
                                assertThat(
                                    context.getBean<PasswordEncoder>().matches(
                                        this@updatedPassword,
                                        password
                                    )
                                ).isTrue
                                assertThat(
                                    context.getBean<PasswordEncoder>().matches(
                                        testPassword,
                                        password
                                    )
                                ).isFalse

                                client
                                    .post()
                                    .uri(API_CHANGE_PASSWORD_PATH)
                                    .contentType(APPLICATION_PROBLEM_JSON)
                                    .bodyValue(
                                        PasswordChange(
                                            "change-password-wrong-existing-password",
                                            this@updatedPassword
                                        )
                                    ).exchange()
                                    .expectStatus()
                                    .isBadRequest
                                    .returnResult<ProblemDetail>()
                                    .responseBodyContent!!
                                    .responseToString()
                                    .run(::assertThat)
                                    .contains(InvalidPasswordException().message)

                                context.findOne<User>(testLogin)
                                    .getOrNull()!!
                                    .run {
                                        assertThat(
                                            context.getBean<PasswordEncoder>().matches(
                                                this@updatedPassword,
                                                password
                                            )
                                        ).isTrue
                                        assertThat(
                                            context.getBean<PasswordEncoder>().matches(
                                                testPassword,
                                                password
                                            )
                                        ).isFalse
                                    }
                            }
                    }
                }
        }
    }

    @Test
    @WithMockUser("change-password", roles = [ROLE_USER])
    fun `test controller change password`(): Unit = runBlocking {
        user.id.run(::assertNull)
        val testLogin = "change-password"
        val testPassword = "change-password"
        context.getBean<Validator>()
            .validateProperty(user.copy(login = testLogin), LOGIN_ATTR)
            .run(::assertThat)
            .isEmpty()
        context.getBean<Validator>()
            .validateProperty(
                PasswordChange(
                    currentPassword = testPassword,
                    newPassword = user.password
                ), CURRENT_PASSWORD_ATTR
            )
            .run(::assertThat)
            .isEmpty()
        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }
    }


    @Test
    @WithMockUser("change-password-too-small")
    fun testChangePasswordTooSmall(): Unit = runBlocking {
        val testLogin = "change-password-too-small"
        val testPassword = "password-too-small"
        user.id.run(::assertNull)
        context.getBean<Validator>()
            .validateProperty(user.copy(login = testLogin), LOGIN_ATTR)
            .run(::assertThat)
            .isEmpty()
        context.getBean<Validator>().validateProperty(
            PasswordChange(
                currentPassword = testPassword,
                newPassword = user.password
            ),
            CURRENT_PASSWORD_ATTR
        ).run(::assertThat).isEmpty()

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup().getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertThat(context.countUsers()).isEqualTo(first + 1)
            assertThat(context.countUserActivation()).isEqualTo(second + 1)
            assertThat(context.countUserAuthority()).isEqualTo(third + 1)

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password attempt: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        message = "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "passwords should match"
                    )

                    "1*2".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )
                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .header(ACCEPT_LANGUAGE, ENGLISH.language)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isBadRequest
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .apply { assertThat(isNotEmpty()).isTrue }
                            .responseToString()
                            .run(::assertThat)
                            .contains("size must be between $PASSWORD_MIN and $PASSWORD_MAX")
                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isFalse
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isTrue
                        }
                    }
                }
        }
    }

    @Test
    @WithMockUser("change-password-too-long")
    fun testChangePasswordTooLong(): Unit = runBlocking {
        val testLogin = "change-password-too-long"
        val testPassword = "password-too-long"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

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
    }

    @Test
    @WithMockUser("change-password-empty")
    fun testChangePasswordEmpty(): Unit = runBlocking {
        val testLogin = "change-password-empty"
        val testPassword = "change-password-empty"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

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
    }


    @Test
    @WithMockUser("change-password")
    fun `test Request Password Reset`(): Unit = runBlocking {
        val testLogin = "change-password"
        val testPassword = "change-password"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

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
    }


    @Test
    @WithMockUser("change-password")
    fun `test Request Password Reset UpperCaseEmail`(): Unit = runBlocking {
        val testLogin = "change-password"
        val testPassword = "change-password"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

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
    }

    @Test
    @WithMockUser("change-password")
    fun `test Request Password Reset Wrong Email`(): Unit = runBlocking {
        val testLogin = "change-password"
        val testPassword = "change-password"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

////        accountWebTestClient.post().uri("/api/account/reset-password/init")
////            .bodyValue("password-reset-wrong-email@example.com")
////            .exchange()
////            .expectStatus().isOk
    }

    @Test
//    @Throws(Exception::class)
    @WithMockUser("change-password")
    fun `test Finish Password Reset`(): Unit = runBlocking {
        val testLogin = "change-password"
        val testPassword = "change-password"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

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
    }

    @Test
//    @Throws(Exception::class)
    @WithMockUser("change-password")
    fun `test Finish Password Reset Too Small`(): Unit = runBlocking {
        val testLogin = "change-password"
        val testPassword = "change-password"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

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
    }

    @Test
    @Throws(Exception::class)
    @WithMockUser("change-password")
    fun `test Finish Password Reset Wrong Key`(): Unit = runBlocking {
        val testLogin = "change-password"
        val testPassword = "change-password"

        user.id.run(::assertNull)

        context.tripleCounts().run {
            val uuid: UUID = (user.copy(
                login = testLogin,
                password = testPassword
            ) to context).signup()
                .getOrNull()!!.first
                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

            assertEquals(first + 1, context.countUsers())
            assertEquals(second + 1, context.countUserActivation())
            assertEquals(third + 1, context.countUserAuthority())

            FIND_ALL_USERS
                .trimIndent()
                .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                .fetch().awaitSingle().run {
                    (this[ID_FIELD].toString().run(::fromString) to this[PASSWORD_FIELD].toString())
                }.run {
                    "user.id retrieved before update password: $first".apply(::i)
                    assertEquals(uuid, first, "user.id should be the same")
                    assertNotEquals(
                        testPassword,
                        second,
                        "password should be encoded and not the same"
                    )
                    assertTrue(
                        context.getBean<PasswordEncoder>().matches(testPassword, second),
                        message = "password should not be different"
                    )

                    "*updatedPassword123".run updatedPassword@{
                        assertEquals(testLogin, getCurrentUserLogin())
                        assertTrue(
                            context.getBean<PasswordEncoder>().matches(
                                testPassword, FIND_ALL_USERS
                                    .trimIndent()
                                    .run(context.getBean<R2dbcEntityTemplate>().databaseClient::sql)
                                    .fetch()
                                    .awaitSingle()[PASSWORD_FIELD]
                                    .toString()
                                    .also { i("password retrieved after user update: $it") }
                            ).apply { "passwords matches : ${toString()}".run(::i) },
                            message = "password should be updated"
                        )

                        client
                            .post()
                            .uri(API_CHANGE_PASSWORD_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .bodyValue(PasswordChange(testPassword, this))
                            .exchange()
                            .expectStatus()
                            .isOk
                            .returnResult<ProblemDetail>()
                            .responseBodyContent!!
                            .isEmpty()
                            .run(::assertTrue)

                        context.findOne<User>(testLogin).getOrNull()!!.run {
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    this@updatedPassword,
                                    password
                                )
                            ).isTrue
                            assertThat(
                                context.getBean<PasswordEncoder>().matches(
                                    testPassword,
                                    password
                                )
                            ).isFalse
                        }
                    }
                }
        }

////        val keyAndPassword = KeyAndPasswordVM(key = "wrong reset key", newPassword = "new password")
////
////        accountWebTestClient.post().uri("/api/account/reset-password/finish")
////            .contentType(MediaType.APPLICATION_JSON)
////            .bodyValue(convertObjectToJsonBytes(keyAndPassword))
////            .exchange()
////            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `test sendEmail`() {
        mailService.sendEmail(
            to = "john.doe@acme.com",
            subject = "testSubject",
            content = "testContent",
            isMultipart = false,
            isHtml = false
        )
        verify(javaMailSender).send(messageCaptor.capture())
        messageCaptor.value.run {
            i("Mime message content: $content")
            assertThat(subject).isEqualTo("testSubject")
            assertThat(allRecipients[0]).hasToString("john.doe@acme.com")
            assertThat(from[0]).hasToString(context.getBean<Properties>().mail.from)
            assertThat(content).isInstanceOf(String::class.java)
            assertThat(content).hasToString("testContent")
            assertThat(dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
        }
    }

    @Test
    fun `test sendMail SendHtmlEmail`() {
        mailService.sendEmail(
            to = "john.doe@acme.com",
            subject = "testSubject",
            content = "testContent",
            isMultipart = false,
            isHtml = true
        )
        verify(javaMailSender).send(messageCaptor.capture())
        messageCaptor.value.run {
            assertThat(subject).isEqualTo("testSubject")
            assertThat("${allRecipients[0]}").isEqualTo("john.doe@acme.com")
            assertThat("${from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
            assertThat(content).isInstanceOf(String::class.java)
            assertThat(content.toString()).isEqualTo("testContent")
            assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
        }
    }

    @Test
    fun `test sendMail SendMultipartEmail`() {
        mailService.sendEmail(
            to = "john.doe@acme.com",
            subject = "testSubject",
            content = "testContent",
            isMultipart = true,
            isHtml = false
        )
        verify(javaMailSender).send(messageCaptor.capture())
        val message = messageCaptor.value
        val part = ((message.content as MimeMultipart)
            .getBodyPart(0).content as MimeMultipart)
            .getBodyPart(0) as MimeBodyPart
        val baos = ByteArrayOutputStream()
        part.writeTo(baos)
        assertThat(message.subject).isEqualTo("testSubject")
        assertThat("${message.allRecipients[0]}").isEqualTo("john.doe@acme.com")
        assertThat("${message.from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
        assertThat(message.content).isInstanceOf(Multipart::class.java)
        assertThat("$baos").isEqualTo("\r\ntestContent")
        assertThat(part.dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
    }

    @Test
    fun `test sendMail SendMultipartHtmlEmail`() {
        mailService.sendEmail(
            to = "john.doe@acme.com",
            subject = "testSubject",
            content = "testContent",
            isMultipart = true,
            isHtml = true
        )
        verify(javaMailSender).send(messageCaptor.capture())
        val message = messageCaptor.value
        val part = ((message.content as MimeMultipart)
            .getBodyPart(0).content as MimeMultipart)
            .getBodyPart(0) as MimeBodyPart
        val aos = ByteArrayOutputStream()
        part.writeTo(aos)
        assertThat(message.subject).isEqualTo("testSubject")
        assertThat("${message.allRecipients[0]}").isEqualTo("john.doe@acme.com")
        assertThat("${message.from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
        assertThat(message.content).isInstanceOf(Multipart::class.java)
        assertThat("$aos").isEqualTo("\r\ntestContent")
        assertThat(part.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    fun `test SendEmailFromTemplate`() {
        user.copy(
            login = "john",
            email = "john.doe@acme.com",
            langKey = "en"
        ).run {
            mailService.sendEmailFromTemplate(
                mapOf(User.objectName to this),
                "mail/testEmail",
                "email.test.title"
            )
            verify(javaMailSender).send(messageCaptor.capture())
            messageCaptor.value.run {
                assertThat(subject).isEqualTo("test title")
                assertThat("${allRecipients[0]}").isEqualTo(email)
                assertThat("${from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
                assertThat(content.toString()).isEqualToNormalizingNewlines(
                    "<html>test title, http://127.0.0.1:8080, john</html>"
                )
                assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
            }
        }
    }

    @Test
    fun testSendEmailWithException() {
        doThrow(MailSendException::class.java)
            .`when`(javaMailSender)
            .send(any(MimeMessage::class.java))
        try {
            mailService.sendEmail(
                "john.doe@acme.com",
                "testSubject",
                "testContent",
                isMultipart = false,
                isHtml = false
            )
        } catch (e: Exception) {
            fail<String>("Exception shouldn't have been thrown")
        }
    }

    @Test
    fun testSendLocalizedEmailForAllSupportedLanguages() {
        user.copy(login = "john", email = "john.doe@acme.com").run {
            for (langKey in languages) {
                mailService.sendEmailFromTemplate(
                    mapOf(User.objectName to copy(langKey = langKey)),
                    "mail/testEmail",
                    "email.test.title"
                )
                verify(javaMailSender, atLeastOnce()).send(messageCaptor.capture())
                val message = messageCaptor.value

                val resource = this::class.java.classLoader.getResource(
                    "i18n/messages_${
                        getJavaLocale(langKey)
                    }.properties"
                )
                assertNotNull(resource)
                val prop = Properties()
                prop.load(
                    InputStreamReader(
                        FileInputStream(File(URI(resource.file).path)), Charset.forName("UTF-8")
                    )
                )
                val emailTitle = prop["email.test.title"] as String
                assertThat(message.subject).isEqualTo(emailTitle)
                assertThat(message.content.toString())
                    .isEqualToNormalizingNewlines("<html>$emailTitle, http://127.0.0.1:8080, john</html>")
            }
        }
    }

    fun getJavaLocale(langKey: String): String {
        var javaLangKey = langKey
        val matcher2 = PATTERN_LOCALE_2.matcher(langKey)
        if (matcher2.matches()) javaLangKey = "${
            matcher2.group(1)
        }_${
            matcher2.group(2).uppercase()
        }"
        val matcher3 = PATTERN_LOCALE_3.matcher(langKey)
        if (matcher3.matches()) javaLangKey = "${
            matcher3.group(1)
        }_${
            matcher3.group(2)
        }_${
            matcher3.group(3).uppercase()
        }"
        return javaLangKey
    }

    @Test
    fun testSendActivationEmail() {
        (user.copy(
            langKey = DEFAULT_LANGUAGE,
            login = "john",
            email = "john.doe@acme.com"
        ) to generateActivationKey).run {
            run(mailService::sendActivationEmail)
            verify(javaMailSender).send(messageCaptor.capture())
            messageCaptor.value.run {
                assertThat("${allRecipients[0]}").isEqualTo(first.email)
                assertThat("${from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
                assertThat(content.toString()).isNotEmpty
                assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
            }
        }
    }

    @Test
    fun testCreationEmail() {
        (user.copy(
            langKey = DEFAULT_LANGUAGE,
            login = "john",
            email = "john.doe@acme.com",
        ) to generateResetKey).run {
            run(mailService::sendCreationEmail)
            verify(javaMailSender).send(messageCaptor.capture())
            messageCaptor.value
                .apply { i("Mime message content: $content") }
                .run {
                    assertThat("${allRecipients[0]}").isEqualTo(first.email)
                    assertThat("${from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
                    assertThat(content.toString()).isNotEmpty
                    assertThat(content.toString()).contains(second)
                    assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
                }
        }
    }

    @Test
    fun testSendPasswordResetMail() {
        (user.copy(
            langKey = DEFAULT_LANGUAGE,
            login = "john",
            email = "john.doe@acme.com"
        ) to generateResetKey).run {
            run(mailService::sendPasswordResetMail)
            verify(javaMailSender).send(messageCaptor.capture())
            messageCaptor.value.run {
                assertThat("${allRecipients[0]}").isEqualTo(first.email)
                assertThat("${from[0]}").isEqualTo(context.getBean<Properties>().mail.from)
                assertThat(content.toString()).isNotEmpty
                assertThat(content.toString()).contains(second)
                assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
            }
        }
    }

//    }
    /**
     *    1/ Workspace
     *         a. create a workspace
     *         b. add an entry to the workspace
     *         c. remove an entry from the workspace
     *         d. update an entry in the workspace
     *         e. find an entry in the workspace
     *     2/ WorkspaceEntry
     *         a. create an Education
     *         b. create an Office
     *         c. create a Job
     *         d. create a Configuration
     *         e. create a Communication
     *         f. create an Organisation
     *         g. create a Collaboration
     *         h. create a Dashboard
     *         i. create a Portfolio
     *     3/ name
     *     4/ office
     *     5/ cores
     *     6/ job
     *     7/ configuration
     *     8/ communication
     *     9/ organisation
     *     10/ collaboration
     *     11/ dashboard
     *     12/ portfolio
     */
    @Nested
    @TestInstance(PER_CLASS)
    inner class WorkspaceTest {
        /**
         * 1 - Créer la configuration du workspace
         * 2 - Ajouter les repositories a la configuration
         * 2 - Cloner les repositories du workspace
         * 3 - Créer les dossiers complémentaires du workspace
         * 4 - lancer les tests gradle
         */
        private val workspace = Workspace(
            entries = WorkspaceEntry(
                name = "fonderie",
                path = System.getProperty(USER_HOME_KEY)
                    .run { "$this/workspace/school" },
                office = Office(
                    books = Books(name = "books-collection"),
                    datas = Datas(name = "datas"),
                    formations = TrainingCatalogue(catalogue = "formations"),
                    bizness = Profession("bizness"),
                    notebooks = Notebooks(notebooks = "notebooks"),
                    pilotage = Pilotage(name = "pilotage"),
                    schemas = Schemas(name = "schemas"),
                    slides = Slides(
                        path = System.getProperty(USER_HOME_KEY)
                            .run { "$this/workspace/office/slides" }),
                    sites = Sites(name = "sites"),
                    path = "office"
                ),
                cores = mapOf(
                    "education" to Education(
                        school = School(name = "talaria"),
                        student = Student(name = "olivier"),
                        teacher = Teacher(name = "cheroliv"),
                        educationTools = EducationTools(name = "edTools")
                    ),
                ),
                job = Job(
                    position = Position("Teacher"),
                    resume = Resume(name = "CV")
                ),
                configuration = Configuration(configuration = "school-configuration"),
                communication = Communication(site = "static-website"),
                organisation = Organisation(organisation = "organisation"),
                collaboration = Collaboration(collaboration = "collaboration"),
                dashboard = Dashboard(dashboard = "dashboard"),
                portfolio = Portfolio(
                    mutableMapOf(
                        "school" to PortfolioProject(
                            name = "name",
                            cred = "credential",
                            builds = mutableMapOf(
                                "training" to ProjectBuild(
                                    name = "training"
                                )
                            )
                        )
                    )
                ),
            )
        )

        @Test
        fun checkDisplayWorkspaceStructure(): Unit {
            workspace.toString().run(::i)
            workspace.displayWorkspaceStructure()
        }

        @Test
        fun `install workspace`(): Unit {
            install(
                System.getProperty(USER_HOME_KEY)
                    .run { "$this/workspace/school" })
            // default type : AllInOneWorkspace
            // ExplodedWorkspace
        }

        @Test
        fun `test create workspace with ALL_IN_ONE config`(): Unit {
            val path = "build/workspace"
            val configFileName = "config.yaml"
            path.run(::File).apply {
                when {
                    !exists() -> mkdirs().run(::assertTrue)
                }
            }.run {
                exists().run(::assertTrue)
                isDirectory.run(::assertTrue)
                WorkspaceConfig(
                    basePath = toPath(),
                    type = ALL_IN_ONE,
                ).run(WorkspaceManager::createWorkspace)
                entries.forEach { "$this/$it".run(::File).exists().run(::assertTrue) }
                "$path/$configFileName".run(::File).exists().run(::assertTrue)
                deleteRecursively().run(::assertTrue)
            }
        }

        @Test
        fun `test create workspace with SEPARATED_FOLDERS config`(): Unit {
            val workspacePath = "build/workspace"
            val configFileName = "config.yaml"
            val subPaths = mutableMapOf<String, Path>()

            entries.mapIndexed { index, workspaceEntryPath ->
                (workspaceEntryPath to (workspacePath.run(::File)
                    .parentFile
                    .listFiles()?.get(index) ?: "build".run(::File)))
            }.forEach { it: Pair<String, File> ->
                "${it.second}/${it.first}"
                    .run(::File)
                    .apply {
                        subPaths[it.first] = toPath()
                        mkdir()
                    }.isDirectory().run(::assertTrue)
            }

            workspacePath.run(::File)
                .apply { if (!exists()) mkdir().run(::assertTrue) }
                .run {
                    exists().run(::assertTrue)
                    isDirectory.run(::assertTrue)

                    val config = WorkspaceConfig(
                        basePath = toPath(),
                        type = SEPARATED_FOLDERS,
                        subPaths = subPaths,
                        configFileName = configFileName
                    ).run(WorkspaceManager::createWorkspace)

                    "$this/$configFileName"
                        .run(::File)
                        .readText()
                        .apply { assertTrue(isNotBlank()) }
                        .run { "config :\n$this" }
                        .run(::i)
                    assertEquals(
                        expected = config.subPaths["office"]!!.pathString,
                        actual = config.workspace.entries.office.path
                    )
                    assertEquals(
                        expected = config.subPaths["education"]!!.pathString,
                        actual = (config.workspace.entries.cores["education"] as Education).path
                    )
                    assertEquals(
                        expected = config.subPaths["communication"]!!.pathString,
                        actual = (config.workspace.entries.communication as Communication).path
                    )
                    assertEquals(
                        expected = config.subPaths["configuration"]!!.pathString,
                        actual = (config.workspace.entries.configuration as Configuration).path
                    )
                    assertEquals(
                        expected = config.subPaths["job"]!!.pathString,
                        actual = (config.workspace.entries.job as Job).path
                    )

                    "$this/$configFileName".run(::File).exists().run(::assertTrue)
                    deleteRecursively().run(::assertTrue)
                }
            subPaths.map {
                it.value.toAbsolutePath().toFile().deleteRecursively().run(::assertTrue)
            }
        }
    }

    //@org.junit.jupiter.api.extension.ExtendWith
//@org.junit.jupiter.api.TestInstance(PER_CLASS)
    class GUITest {
        private lateinit var window: FrameFixture

        //    @kotlin.test.BeforeTest
        fun setUp() = execute {
            window =
//            context.
                run(Installer::GUI)
                    .run(::FrameFixture)
                    .apply(FrameFixture::show)
        }

        //    @kotlin.test.AfterTest
        fun tearDown() = window.cleanUp()
    }
}