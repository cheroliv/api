@file:Suppress("RedundantUnitReturnType")

package app

import app.TestUtils.Data.DEFAULT_USER_JSON
import app.TestUtils.Data.OFFICIAL_SITE
import app.TestUtils.Data.signup
import app.TestUtils.Data.user
import app.TestUtils.Data.users
import app.TestUtils.FIND_ALL_USERACTIVATION
import app.TestUtils.FIND_ALL_USER_RESETS
import app.TestUtils.FIND_BY_ACTIVATION_KEY
import app.TestUtils.changePasswordScenario
import app.TestUtils.countRoles
import app.TestUtils.countUserActivation
import app.TestUtils.countUserAuthority
import app.TestUtils.countUserResets
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
import app.TestUtils.resetPasswordScenario
import app.TestUtils.responseToString
import app.TestUtils.signupActivationScenario
import app.TestUtils.tripleCounts
import app.ai.AIAssistantManager
import app.ai.AIAssistantManager.SimpleAiController.LocalLLMModel.ollamaList
import app.users.api.Constants
import app.users.api.Constants.DEVELOPMENT
import app.users.api.Constants.EMPTY_STRING
import app.users.api.Constants.PRODUCTION
import app.users.api.Constants.ROLE_USER
import app.users.api.Constants.STARTUP_LOG_MSG_KEY
import app.users.api.Constants.USER
import app.users.api.Loggers.i
import app.users.api.Properties
import app.users.api.Utils.lsWorkingDir
import app.users.api.Utils.lsWorkingDirProcess
import app.users.api.Utils.toJson
import app.users.api.dao.UserDao.availability
import app.users.api.dao.UserDao.change
import app.users.api.dao.UserDao.findOne
import app.users.api.dao.UserDao.save
import app.users.api.dao.UserDao.signup
import app.users.api.dao.UserDao.user
import app.users.api.mail.MailService
import app.users.api.mail.SMTPMailService
import app.users.api.models.EntityModel
import app.users.api.models.EntityModel.Members.withId
import app.users.api.models.Role
import app.users.api.models.User
import app.users.api.models.User.Attributes.EMAIL_ATTR
import app.users.api.models.User.Attributes.LOGIN_ATTR
import app.users.api.models.User.Relations.FIND_ALL_USERS
import app.users.api.models.User.Relations.Fields.EMAIL_FIELD
import app.users.api.models.User.Relations.Fields.LANG_KEY_FIELD
import app.users.api.models.User.Relations.Fields.LOGIN_FIELD
import app.users.api.models.User.Relations.Fields.PASSWORD_FIELD
import app.users.api.models.User.Relations.Fields.VERSION_FIELD
import app.users.api.models.UserRole
import app.users.api.security.SecurityUtils
import app.users.api.security.SecurityUtils.generateActivationKey
import app.users.api.security.SecurityUtils.getCurrentUserLogin
import app.users.api.web.HttpUtils.validator
import app.users.password.InvalidPasswordException
import app.users.password.PasswordChange
import app.users.password.PasswordService
import app.users.password.ResetPassword
import app.users.password.UserReset
import app.users.password.UserReset.EndPoint.API_CHANGE_PASSWORD_PATH
import app.users.password.UserReset.EndPoint.API_RESET_PASSWORD_INIT_PATH
import app.users.password.UserReset.Relations.Fields.IS_ACTIVE_FIELD
import app.users.password.UserReset.Relations.Fields.RESET_KEY_FIELD
import app.users.signup.Signup
import app.users.signup.Signup.EndPoint.API_ACTIVATE_PARAM
import app.users.signup.Signup.EndPoint.API_ACTIVATE_PATH
import app.users.signup.SignupDao.activate
import app.users.signup.SignupDao.validate
import app.users.signup.SignupService
import app.users.signup.SignupService.Companion.ONE_ROW_UPDATED
import app.users.signup.UserActivation
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_DATE_FIELD
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_KEY_FIELD
import app.users.signup.UserActivation.Relations.Fields.CREATED_DATE_FIELD
import app.workspace.Installer
import app.workspace.Workspace
import app.workspace.WorkspaceManager
import app.workspace.WorkspaceManager.displayWorkspaceStructure
import app.workspace.WorkspaceManager.workspace
import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.mail.Multipart
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.swing.edt.GuiActionRunner
import org.assertj.swing.fixture.FrameFixture
import org.hibernate.validator.HibernateValidator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.security.crypto.encrypt.Encryptors.text
import org.springframework.security.crypto.keygen.KeyGenerators.string
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
import java.lang.Boolean.parseBoolean
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant.now
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime.ofInstant
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.Locale.FRANCE
import java.util.Locale.FRENCH
import java.util.Locale.ITALY
import java.util.Locale.LanguageRange
import java.util.Locale.US
import java.util.Locale.filter
import java.util.UUID
import javax.inject.Inject
import kotlin.io.path.pathString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Tests {
    //@org.junit.jupiter.api.extension.ExtendWith
    //@org.junit.jupiter.api.TestInstance(PER_CLASS)
    class InstallerTests {
        lateinit var window: FrameFixture

        //    @kotlin.test.BeforeTest
        fun setUp() = GuiActionRunner.execute {
            // context.
            window = run(Installer::GUI)
                .run(::FrameFixture)
                .apply(FrameFixture::show)
        }

        //    @kotlin.test.AfterTest
        fun tearDown() = window.cleanUp()
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class WorkspaceTests {
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
         *
         ****************************************************
         *
         * 1 - Créer la configuration du workspace
         * 2 - Ajouter les repositories a la configuration
         * 2 - Cloner les repositories du workspace
         * 3 - Créer les dossiers complémentaires du workspace
         * 4 - lancer les tests gradle
         */
        private val workspace = Workspace(
            entries = Workspace.WorkspaceEntry(
                name = "fonderie",
                path = System.getProperty(SystemUtils.USER_HOME_KEY)
                    .run { "$this/workspace/school" },
                office = Workspace.WorkspaceEntry.OfficeEntry.Office(
                    books = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Books(name = "books-collection"),
                    datas = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Datas(name = "datas"),
                    formations = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.TrainingCatalogue(
                        catalogue = "formations"
                    ),
                    bizness = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Profession("bizness"),
                    notebooks = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Notebooks(
                        notebooks = "notebooks"
                    ),
                    pilotage = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Pilotage(
                        name = "pilotage"
                    ),
                    schemas = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Schemas(name = "schemas"),
                    slides = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Slides(
                        path = System.getProperty(SystemUtils.USER_HOME_KEY)
                            .run { "$this/workspace/office/slides" }),
                    sites = Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.Sites(name = "sites"),
                    path = "office"
                ),
                cores = mapOf(
                    "education" to Workspace.WorkspaceEntry.CoreEntry.Education(
                        school = Workspace.WorkspaceEntry.CoreEntry.Education.EducationEntry.School(
                            name = "talaria"
                        ),
                        student = Workspace.WorkspaceEntry.CoreEntry.Education.EducationEntry.Student(
                            name = "olivier"
                        ),
                        teacher = Workspace.WorkspaceEntry.CoreEntry.Education.EducationEntry.Teacher(
                            name = "cheroliv"
                        ),
                        educationTools = Workspace.WorkspaceEntry.CoreEntry.Education.EducationEntry.EducationTools(
                            name = "edTools"
                        )
                    ),
                ),
                job = Workspace.WorkspaceEntry.JobEntry.Job(
                    position = Workspace.WorkspaceEntry.JobEntry.Job.HumanResourcesEntry.Position("Teacher"),
                    resume = Workspace.WorkspaceEntry.JobEntry.Job.HumanResourcesEntry.Resume(name = "CV")
                ),
                configuration = Workspace.WorkspaceEntry.ConfigurationEntry.Configuration(
                    configuration = "school-configuration"
                ),
                communication = Workspace.WorkspaceEntry.CommunicationEntry.Communication(site = "static-website"),
                organisation = Workspace.WorkspaceEntry.OrganisationEntry.Organisation(organisation = "organisation"),
                collaboration = Workspace.WorkspaceEntry.CollaborationEntry.Collaboration(
                    collaboration = "collaboration"
                ),
                dashboard = Workspace.WorkspaceEntry.DashboardEntry.Dashboard(dashboard = "dashboard"),
                portfolio = Workspace.WorkspaceEntry.PortfolioEntry.Portfolio(
                    mutableMapOf(
                        "school" to Workspace.WorkspaceEntry.PortfolioEntry.Portfolio.PortfolioProject(
                            name = "name",
                            cred = "credential",
                            builds = mutableMapOf(
                                "training" to Workspace.WorkspaceEntry.PortfolioEntry.Portfolio.PortfolioProject.ProjectBuild(
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
            workspace.toString().run(::println)
            workspace.displayWorkspaceStructure()
        }

        @Test
        fun `install workspace`(): Unit {
            Workspace.install(
                System.getProperty(SystemUtils.USER_HOME_KEY)
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
                Workspace.WorkspaceConfig(
                    basePath = toPath(),
                    type = Workspace.InstallationType.ALL_IN_ONE,
                ).run(WorkspaceManager::createWorkspace)
                WorkspaceManager.WorkspaceConstants.entries.forEach {
                    "$this/$it".run(::File).exists().run(::assertTrue)
                }
                "$path/$configFileName".run(::File).exists().run(::assertTrue)
                deleteRecursively().run(::assertTrue)
            }
        }

        @Test
        fun `test create workspace with SEPARATED_FOLDERS config`(): Unit {
            val workspacePath = "build/workspace"
            val configFileName = "config.yaml"
            val subPaths = mutableMapOf<String, Path>()

            WorkspaceManager.WorkspaceConstants.entries.mapIndexed { index, workspaceEntryPath ->
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

                    val config = Workspace.WorkspaceConfig(
                        basePath = toPath(),
                        type = Workspace.InstallationType.SEPARATED_FOLDERS,
                        subPaths = subPaths,
                        configFileName = configFileName
                    ).run(WorkspaceManager::createWorkspace)

//                        "$this$separator$configFileName"
                    "$this/$configFileName"
                        .run(::File)
                        .readText()
                        .apply { assertTrue(isNotBlank()) }
                        .run { "config :\n$this" }
                        .run(::println)
                    assertEquals(
                        expected = config.subPaths["office"]!!.pathString,
                        actual = config.workspace.entries.office.path
                    )
                    assertEquals(
                        expected = config.subPaths["education"]!!.pathString,
                        actual = (config.workspace.entries.cores["education"] as Workspace.WorkspaceEntry.CoreEntry.Education).path
                    )
                    assertEquals(
                        expected = config.subPaths["communication"]!!.pathString,
                        actual = (config.workspace.entries.communication as Workspace.WorkspaceEntry.CommunicationEntry.Communication).path
                    )
                    assertEquals(
                        expected = config.subPaths["configuration"]!!.pathString,
                        actual = (config.workspace.entries.configuration as Workspace.WorkspaceEntry.ConfigurationEntry.Configuration).path
                    )
                    assertEquals(
                        expected = config.subPaths["job"]!!.pathString,
                        actual = (config.workspace.entries.job as Workspace.WorkspaceEntry.JobEntry.Job).path
                    )

//                        "$this$separator$configFileName".run(::File).exists().run(::assertTrue)
                    "$this/$configFileName".run(::File).exists().run(::assertTrue)
                    deleteRecursively().run(::assertTrue)
                }
            subPaths.map {
                it.value.toAbsolutePath().toFile().deleteRecursively().run(::assertTrue)
            }
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @ActiveProfiles("test", "mailbox", "ai")
    @SpringBootTest(
        classes = [Server::class],
        properties = ["spring.main.web-application-type=reactive"]
    )
    inner class ApplicationTests {

        @Inject
        lateinit var context: ApplicationContext

        @Inject
        lateinit var properties: Properties

        @Inject
        lateinit var db: DatabaseClient

        @Inject
        lateinit var environment: Environment

        @Inject
        lateinit var validator: Validator

        @Inject
        lateinit var mapper: ObjectMapper

        @Inject
        lateinit var messageSource: MessageSource

        @Inject
        lateinit var passwordEncoder: PasswordEncoder

        @Inject
        lateinit var transactionalOperator: TransactionalOperator

        @Inject
        lateinit var signupService: SignupService

        @Inject
        lateinit var passwordService: PasswordService

        @Inject
        lateinit var templateEngine: SpringWebFluxTemplateEngine

        lateinit var client: WebTestClient

        @BeforeTest
        fun setUp(context: ApplicationContext): Unit {
            client = context
                .run(WebTestClient::bindToApplicationContext)
                .build()
        }

        @AfterTest
        fun cleanUp(context: ApplicationContext): Unit = runBlocking {
            context.deleteAllUsersOnly()
        }

        @Nested
        @TestInstance(PER_CLASS)
        inner class AiTests {
            @Suppress("MemberVisibilityCanBePrivate", "PropertyName")
            val EXPECTED_KEYWORDS = setOf(
                "```", "code", "function",
                "python", "html", "div", "json",
                "yaml", "xml", "javascript",
                "css", "kotlin", "if", "else",
                "for", "while", "return", "print",
                "true", "false", "program",
            )

            @Test
            fun `test ollama configuration`(): Unit {
                environment["langchain4j.ollama.chat-model.base-url"]
                    .run(::assertThat)
                    .isEqualTo("http://localhost:11434")
                environment["langchain4j.ollama.chat-model.model-name"]
                    .run(::assertThat)
                    .isIn(*ollamaList.toTypedArray())
                ollamaList.run(::assertThat)
                    .contains("smollm:135m")
            }

            @Test
            fun `test simple ai api, json format response`(): Unit = runBlocking {
                client.mutate()
                    .responseTimeout(Duration.ofSeconds(60))
                    .build()
                    .get().uri("/api/ai/simple")
                    .exchange().expectStatus().isOk
                    .returnResult<ResponseEntity<AIAssistantManager.SimpleAiController.AssistantResponse>>()
                    .responseBodyContent!!.responseToString().apply {
                        mapper
                            .readValue<AIAssistantManager.SimpleAiController.AssistantResponse.Success>(
                                this
                            )
                            .answer!!
                            .run(::i)
                    }.run(::assertThat)
                    .isNotBlank()
                    .asString()
                    .containsAnyOf(*EXPECTED_KEYWORDS.toTypedArray())
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        inner class CoreTests {

            @Test
            fun `test properties_mailbox_no-reply_from should be a valid email`()
                    : Unit = assertDoesNotThrow {
                properties.mailbox.noReply.from.apply {
                    validator.validateProperty(user.copy(email = this), EMAIL_ATTR)
                        .run(::assertThat).isEmpty()
                }.run { i("properties.mailbox.no-reply.from: $this") }
            }

            @Test
            fun `test symmetric encryption and decryption`(): Unit = assertDoesNotThrow {
                val salt =
                    string().generateKey().apply { "salt: $this".run(::i) }
                val encryptor = text("RGPD", salt)
                encryptor.encrypt(user.email.lowercase()).apply(::i).run {
                    encryptor.decrypt(this)
                        .apply(::i)
                        .run(::assertThat).asString()
                        .isEqualTo(user.email.lowercase())
                }
            }

//            @Test
//            fun `test encryption secret properties`(): Unit {
//                properties
//                    .encrypter
//                    .secret
//                    .run(::assertThat)
//                    .asString()
//                    .isEqualTo("RGPD")
//            }


            @Test
            fun `DataTestsChecks - display some json`(): Unit = assertDoesNotThrow {
                mapper.run {
                    writeValueAsString(users).run(::i)
                    writeValueAsString(user).run(::i)
                }
                DEFAULT_USER_JSON.run(::i)
            }


            @Test
            fun `ConfigurationsTests - MessageSource test email_activation_greeting message fr`(): Unit =
                System.getProperty("user.name").run {
                    assertThat("Cher $this").isEqualTo(
                        messageSource.getMessage(
                            "email.activation.greeting",
                            arrayOf(this), FRENCH
                        )
                    )
                }


            @Test
            fun `ConfigurationsTests - MessageSource test message startupLog`(): Unit {
                assertThat(buildString {
                    append("You have misconfigured your application!\n")
                    append("It should not run with both the $DEVELOPMENT\n")
                    append("and $PRODUCTION profiles at the same time.")
                }).asString().isEqualTo(
                    messageSource.getMessage(
                        STARTUP_LOG_MSG_KEY,
                        arrayOf(DEVELOPMENT, PRODUCTION),
                        Locale.getDefault()
                    ).apply { i(this) })
            }

            @Test
            fun `ConfigurationsTests - test go visit message`(): Unit {
                assertThat(properties.goVisitMessage)
                    .asString()
                    .isEqualTo(OFFICIAL_SITE)
            }

            @Test
            fun `test lsWorkingDir & lsWorkingDirProcess`(): Unit = assertDoesNotThrow {
                "build".let {
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
            }


            @Test
            fun `display user formatted in JSON`(): Unit = assertDoesNotThrow {
                (user to context).toJson.let(::i)
            }

            @Test
            fun `check toJson build a valid json format`(): Unit = assertDoesNotThrow {
                (user to context).toJson.let(mapper::readTree)
            }

            @Test
            fun `Verifies the internationalization of validations by validator factory with a bad login in Italian`(): Unit {
                Validation.byProvider(HibernateValidator::class.java)
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
                    .validateProperty(signup.copy(login = "funky-log(n"), LOGIN_FIELD).run viol@{
                        assertThat(isNotEmpty()).isTrue
                        first().run {
                            assertThat("{${Pattern::class.java.name}.message}")
                                .asString()
                                .isEqualTo(messageTemplate)
                            assertThat(message)
                                .contains("deve corrispondere a \"^(?>[a-zA-Z0-9!\$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)\$\"")
                                .doesNotContain("doit correspondre à")
                        }
                    }
            }

            @Test
            fun `Verify the internationalization of validations by validator factory with a bad login in Italian`(): Unit {
                Validation.byProvider(HibernateValidator::class.java)
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
                    .validateProperty(
                        signup.copy(login = "funky-log(n"),
                        LOGIN_FIELD
                    )
                    .run viol@{
                        assertTrue(isNotEmpty())
                        first().run {
                            assertThat("{${Pattern::class.java.name}.message}")
                                .asString()
                                .isEqualTo(messageTemplate)
                            assertThat(message)
                                .asString()
                                .doesNotContain("doit correspondre à")
                                .contains("deve corrispondere a \"^(?>[a-zA-Z0-9!\$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)\$\"")
                        }
                    }
            }

        }

        @Nested
        @TestInstance(PER_CLASS)
        inner class UserDaoTests {
            @Test
            fun `test findOne`(): Unit = runBlocking {
                assertThat(context.countUsers()).isEqualTo(0)
                (user to context).save()
                assertThat(context.countUsers()).isEqualTo(1)
                val findOneEmailResult: Either<Throwable, User> =
                    context.findOne<User>(user.email)
                findOneEmailResult.map { assertDoesNotThrow { UUID.fromString(it.id.toString()) } }
                i("findOneEmailResult : ${findOneEmailResult.getOrNull()}")
                context.findOne<User>(user.login).map {
                    assertDoesNotThrow { UUID.fromString(it.id.toString()) }
                }
            }

            @Test
            fun `test r2dbc-sql to find user and roles using one query`(): Unit = runBlocking {
                context.tripleCounts().run {
                    run(::assertThat).isEqualTo(Triple(0, 0, 0))
                    (user to context).signup()
                    i(context.countUsers().toString())
                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)
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
                   u."id", u."email", u."login";
                """.trimIndent()
                    .apply(::i)
                    .run(db::sql)
                    .bind("emailOrLogin", user.email)
                    .fetch()
                    .awaitSingleOrNull()?.run {
                        toString().run(::i)
                        val expectedUserResult = User(
                            id = UUID.fromString(get(User.Relations.Fields.ID_FIELD).toString()),
                            email = get(EMAIL_FIELD).toString(),
                            login = get(LOGIN_FIELD).toString(),
                            roles = get(User.Members.ROLES_MEMBER)
                                .toString()
                                .split(",")
                                .map { Role(it) }
                                .toSet(),
                            password = get(PASSWORD_FIELD).toString(),
                            langKey = get(LANG_KEY_FIELD).toString(),
                            version = get(VERSION_FIELD).toString().toLong(),
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
                            passwordEncoder
                                .matches(user.password, expectedUserResult.password)
                            passwordEncoder
                                .matches(user.password, userResult.password)
                        }
                        assertEquals(expectedUserResult.roles.first().id, ROLE_USER)
                        assertEquals(userResult.roles.first().id, ROLE_USER)
                        assertEquals(userResult.roles.size, 1)
                    }
            }


            @Test
            fun `test findOneWithAuths`(): Unit = runBlocking {
                assertThat(0)
                    .isEqualTo(context.countUsers())
                    .isEqualTo(context.countUserAuthority())
                val userId: UUID = (user to context).signup().getOrNull()!!.first
                userId.apply {
                    i("(user to context).signup() : $this")
                    run(::assertNotNull)
                }
                assertThat(1)
                    .isEqualTo(context.countUsers())
                    .isEqualTo(context.countUserAuthority())

                context.findOne<User>(user.email).getOrNull()?.apply {
                    run(::assertNotNull)
                    assertEquals(1, roles.size)
                    assertEquals(ROLE_USER, roles.first().id)
                    assertEquals(userId, id)
                }.run { i("context.findOneWithAuths<User>(${user.email}).getOrNull() : $this") }

                context.findOne<User>(userId).getOrNull()
                    .run { i("context.findOneDraft<User>(user.email).getOrNull() : $this") }
                context.findAuthsByEmail(user.email).getOrNull()
                    .run { i("context.findAuthsByEmail(${user.email}).getOrNull() : $this") }
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
                    userWithAuths =
                        user.withId(it.first).copy(password = EMPTY_STRING)
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
                    userWithAuths =
                        user.withId(it.first).copy(password = EMPTY_STRING)
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
                    userWithAuths =
                        user.withId(it.first).copy(password = EMPTY_STRING)
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
                WHERE u."email" = :email;
                """.trimIndent()
                        .run(db::sql)
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
                            .run(db::sql)
                            .bind(UserRole.Attributes.USER_ID_ATTR, it.first)
                            .fetch()
                            .all()
                            .collect { rows ->
                                assertEquals(
                                    rows[Role.Relations.Fields.ID_FIELD],
                                    ROLE_USER
                                )
                                resultRoles.add(Role(id = rows[Role.Relations.Fields.ID_FIELD].toString()))
                            }
                        assertEquals(
                            ROLE_USER,
                            user.withId(it.first).copy(
                                roles =
                                resultRoles
                                    .map { role -> role.id.run(::Role) }
                                    .toMutableSet())
                                .roles.first().id
                        )
                        resultUserId = it.first
                    }
                    assertThat(resultUserId.toString().length)
                        .isEqualTo(UUID.randomUUID().toString().length)
                    assertDoesNotThrow { UUID.fromString(resultUserId.toString()) }
                    assertThat(ROLE_USER).isEqualTo(resultRoles.first().id)
                    assertThat(context.countUsers()).isEqualTo(1)
                    assertThat(context.countUserAuthority()).isEqualTo(1)
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
                }.map { assertDoesNotThrow { UUID.fromString(it.id.toString()) } }
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
                    TestUtils.FIND_USER_BY_LOGIN
                        .run(db::sql)
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

        }

        @Nested
        @TestInstance(PER_CLASS)
        inner class EmailSendingTests {
            lateinit var mailService: MailService

            @Spy
            lateinit var javaMailSender: JavaMailSenderImpl

            @Captor
            lateinit var messageCaptor: ArgumentCaptor<MimeMessage>

            @BeforeTest
            fun setUpEmailSending(context: ApplicationContext) {
                MockitoAnnotations.openMocks(this)
                doNothing()
                    .`when`(javaMailSender)
                    .send(ArgumentMatchers.any(MimeMessage::class.java))
                mailService = SMTPMailService(
                    properties,
                    javaMailSender,
                    messageSource,
                    templateEngine
                )
            }

            @Test
            fun `test sendEmail`(): Unit {
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
                    assertThat(from[0])
                        .hasToString(properties.mailbox.noReply.from)
                    assertThat(content).isInstanceOf(String::class.java)
                    assertThat(content).hasToString("testContent")
                    assertThat(dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
                }
            }

            @Test
            fun `test sendMail SendHtmlEmail`(): Unit {
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
                    assertThat("${from[0]}")
                        .isEqualTo(properties.mailbox.noReply.from)
                    assertThat(content).isInstanceOf(String::class.java)
                    assertThat(content.toString()).isEqualTo("testContent")
                    assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
                }
            }

            @Test
            fun `test sendMail SendMultipartEmail`(): Unit {
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
                assertThat("${message.from[0]}")
                    .isEqualTo(properties.mailbox.noReply.from)
                assertThat(message.content).isInstanceOf(Multipart::class.java)
                assertThat("$baos").isEqualTo("\r\ntestContent")
                assertThat(part.dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
            }

            @Test
            fun `test sendMail SendMultipartHtmlEmail`(): Unit {
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
                assertThat("${message.from[0]}")
                    .isEqualTo(properties.mailbox.noReply.from)
                assertThat(message.content).isInstanceOf(Multipart::class.java)
                assertThat("$aos").isEqualTo("\r\ntestContent")
                assertThat(part.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
            }

            @Test
            fun `test SendEmailFromTemplate`(): Unit {
                user.copy(
                    login = "john",
                    email = "john.doe@acme.com",
                    langKey = "en"
                ).run {
                    mailService.sendEmailFromTemplate(
                        mapOf(User.objectName to this),
                        "mail/testEmail",
                        "email.activation.title"
                    )
                    verify(javaMailSender).send(messageCaptor.capture())
                    messageCaptor.value.run {
                        assertThat(subject)
                            .isEqualTo("school account activation")//.isEqualTo("Account activation")
                        assertThat("${allRecipients[0]}").isEqualTo(email)
                        assertThat("${from[0]}")
                            .isEqualTo(properties.mailbox.noReply.from)
                        assertThat(content.toString()).isEqualToNormalizingNewlines(
                            "<html>test title, http://127.0.0.1:8880, john</html>"
                        )
                        assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
                    }
                }
            }

            @Test
            fun testSendEmailWithException(): Unit {
                Mockito.doThrow(MailSendException::class.java)
                    .`when`(javaMailSender)
                    .send(ArgumentMatchers.any(MimeMessage::class.java))
                try {
                    mailService.sendEmail(
                        "john.doe@acme.com",
                        "testSubject",
                        "testContent",
                        isMultipart = false,
                        isHtml = false
                    )
                } catch (e: Exception) {
                    Assertions.fail<String>("Exception shouldn't have been thrown")
                }
            }

            @Test
            fun testSendLocalizedEmailForAllSupportedLanguages(): Unit {
                user.copy(login = "john", email = "john.doe@acme.com").run {
                    for (langKey in Constants.languages) {
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
                        val prop = java.util.Properties()
                        prop.load(
                            InputStreamReader(
                                FileInputStream(File(URI(resource.file).path)),
                                Charset.forName("UTF-8")
                            )
                        )
                        val emailTitle = prop["email.test.title"] as String
                        assertThat(message.subject).isEqualTo(emailTitle)
                        assertThat(message.content.toString())
                            .isEqualToNormalizingNewlines("<html>$emailTitle, http://127.0.0.1:${context.environment["server.port"]}, john</html>")
                    }
                }
            }

            fun getJavaLocale(langKey: String): String {
                var javaLangKey = langKey
                val matcher2 = Constants.PATTERN_LOCALE_2.matcher(langKey)
                if (matcher2.matches()) javaLangKey = "${
                    matcher2.group(1)
                }_${
                    matcher2.group(2).uppercase()
                }"
                val matcher3 = Constants.PATTERN_LOCALE_3.matcher(langKey)
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
            fun testSendActivationEmail(): Unit {
                (user.copy(
                    langKey = Constants.DEFAULT_LANGUAGE,
                    login = "john",
                    email = "john.doe@acme.com"
                ) to generateActivationKey).run {
                    run(mailService::sendActivationEmail)
                    verify(javaMailSender).send(messageCaptor.capture())
                    messageCaptor.value.run {
                        assertThat("${allRecipients[0]}").isEqualTo(first.email)
                        assertThat("${from[0]}")
                            .isEqualTo(properties.mailbox.noReply.from)
                        assertThat(content.toString()).isNotEmpty
                        assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
                        content.toString()
                            .run { i("Activation mail Mime message content: $this") }
                    }
                }
            }

            @Test
            fun testCreationEmail(): Unit {
                (user.copy(
                    langKey = Constants.DEFAULT_LANGUAGE,
                    login = "john",
                    email = "john.doe@acme.com",
                ) to SecurityUtils.generateResetKey).run {
                    run(mailService::sendCreationEmail)
                    verify(javaMailSender).send(messageCaptor.capture())
                    messageCaptor.value
                        .apply { i("Activation key: $second") }
                        .apply { i("Mime message content(activation mail): $content") }
                        .run {
                            assertThat("${allRecipients[0]}").isEqualTo(first.email)
                            assertThat("${from[0]}")
                                .isEqualTo(properties.mailbox.noReply.from)
                            assertThat(content.toString()).isNotEmpty
                            assertThat(content.toString()).contains(second)
                            assertThat(dataHandler.contentType)
                                .isEqualTo("text/html;charset=UTF-8")
                        }
                }
            }

            @Test
            fun testSendPasswordResetMail(): Unit {
                (user.copy(
                    langKey = Constants.DEFAULT_LANGUAGE,
                    login = "john",
                    email = "john.doe@acme.com"
                ) to SecurityUtils.generateResetKey).run {
                    run(mailService::sendPasswordResetMail)
                    verify(javaMailSender).send(messageCaptor.capture())
                    messageCaptor.value.run {
                        assertThat("${allRecipients[0]}").isEqualTo(first.email)
                        assertThat("${from[0]}")
                            .isEqualTo(properties.mailbox.noReply.from)
                        assertThat(content.toString()).isNotEmpty
                        assertThat(content.toString()).contains(second)
                        assertThat(dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
                    }
                }
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        inner class UserResetPasswordTests {

            @Test
            fun `test dao update user password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)

                assertThat(userTest.id).isNull()

                context.tripleCounts().run {
                    val uuid: UUID = (userTest to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                userTest.password,
                                second,
                                "password should be different"
                            )
                            assertTrue(
                                passwordEncoder
                                    .matches(userTest.password, second),
                                message = "password should be encoded"
                            )

                            "*updatedPassword123".run {
                                (userTest.copy(id = first, password = this) to context)
                                    .change()
                                    .apply { assertFalse(isLeft()) }
                                    .map {
                                        i("row updated : $it")
                                        assertEquals(ONE_ROW_UPDATED, it)
                                    }
                                assertTrue(
                                    passwordEncoder.matches(
                                        this, FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
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
            @WithMockUser(username = USER, roles = [ROLE_USER])
            fun `test service update user password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = User(
                    login = signupTest.login,
                    email = signupTest.email,
                    password = signupTest.password,
                    langKey = FRENCH.language
                )
                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }
                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                userTest.password,
                                second,
                                "password should be different"
                            )
                            assertTrue(
                                passwordEncoder
                                    .matches(userTest.password, second),
                                message = "password should be encoded"
                            )

                            "*updatedPassword123".run {
                                assertThat(getCurrentUserLogin())
                                    .isEqualTo(userTest.login)
                                assertEquals(
                                    ONE_ROW_UPDATED,
                                    passwordService
                                        .change(PasswordChange(signupTest.password, this))
                                )
                                assertTrue(
                                    passwordEncoder.matches(
                                        this, FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
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
            fun `test change password with wrong existing password, only service layer`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = USER,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = User(
                        login = signupTest.login,
                        email = signupTest.email,
                        password = signupTest.password,
                        langKey = FRENCH.language
                    )
                    assertThat(userTest.id).isNull()
                    context.tripleCounts().run {
                        val uuid: UUID = (userTest to context).signup()
                            .getOrNull()!!.first
                            .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                        assertThat(context.countUsers()).isEqualTo(first + 1)
                        assertThat(context.countUserActivation()).isEqualTo(third + 1)
                        assertThat(context.countUserAuthority()).isEqualTo(second + 1)

                        FIND_ALL_USERS
                            .trimIndent()
                            .run(db::sql)
                            .fetch().awaitSingle().run {
                                (this[User.Relations.Fields.ID_FIELD].toString()
                                    .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                            }.run {
                                "user.id retrieved before update password: $first".apply(::i)
                                assertEquals(uuid, first, "user.id should be the same")
                                assertNotEquals(
                                    userTest.password,
                                    second,
                                    "password should be different"
                                )
                                assertTrue(
                                    passwordEncoder
                                        .matches(userTest.password, second),
                                    message = "password should be encoded"
                                )

                                "*updatedPassword123".run {
                                    assertNotEquals(
                                        userTest.login,
                                        getCurrentUserLogin()
                                    )
                                    assertThrows<InvalidPasswordException> {
                                        passwordService.change(
                                            PasswordChange(
                                                userTest.password,
                                                this
                                            )
                                        )
                                    }
                                }
                            }
                    }
                }

            @Test
            @WithMockUser("change-password-wrong-existing-password")
            fun `test change password with wrong existing password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                val testLogin = "change-password-wrong-existing-password"
                val testPassword = "changePasswordWrong*"
                assertThat(userTest.id).isNull()
                context.tripleCounts().run triple@{
                    val uuid: UUID = (userTest.copy(
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
                        .run(db::sql)
                        .fetch()
                        .awaitSingle()
                        .run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }
                        .run pairUuidPassword@{
                            i("user.id retrieved before update password: ${this@pairUuidPassword.first}")
                            assertEquals(
                                uuid,
                                this@pairUuidPassword.first,
                                "user.id should be the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(testPassword, second),
                                message = "password should be encoded and match"
                            )
                            "*updatedPassword123".run updatedPassword@{
                                assertThat(getCurrentUserLogin())
                                    .isNotEqualTo(this@updatedPassword)
                                    .isEqualTo(testLogin)
                                passwordService.change(
                                    PasswordChange(
                                        testPassword,
                                        this
                                    )
                                )
                                context.findOne<User>(testLogin)
                                    .apply { assertThat(isRight()).isTrue }
                                    .getOrNull()!!.run {
                                        assertThat(
                                            passwordEncoder.matches(
                                                this@updatedPassword,
                                                password
                                            )
                                        ).isTrue
                                        assertThat(
                                            passwordEncoder.matches(
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

                                        context.findOne<User>(testLogin).getOrNull()!!.run {
                                            assertThat(
                                                passwordEncoder.matches(
                                                    this@updatedPassword,
                                                    password
                                                )
                                            ).isTrue
                                            assertThat(
                                                passwordEncoder.matches(
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
            fun `test change password with valid password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                assertThat(userTest.id).isNull()

                val testLogin = "change-password"
                val testPassword = "change-password"

                validator.validateProperty(
                    userTest.copy(login = testLogin),
                    LOGIN_ATTR
                ).run(::assertThat).isEmpty()

                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = userTest.password
                    ), PasswordChange.Attributes.CURRENT_PASSWORD_ATTR
                ).run(::assertThat).isEmpty()

                context.tripleCounts().run {
                    val uuid: UUID = (userTest.copy(
                        login = testLogin,
                        password = testPassword
                    ) to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                testPassword,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(testPassword, second),
                                message = "password should not be different"
                            )

                            "*updatedPassword123".run updatedPassword@{
                                assertEquals(testLogin, getCurrentUserLogin())
                                assertTrue(
                                    passwordEncoder.matches(
                                        testPassword, FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
                                            .fetch()
                                            .awaitSingle()[PASSWORD_FIELD]
                                            .toString()
                                            .also { i("password retrieved after user update: $it") }
                                    ).apply { "passwords matches : ${toString()}".run(::i) },
                                    message = "password should be updated"
                                )

                                transactionalOperator.executeAndAwait {
                                    assertDoesNotThrow {
                                        passwordService
                                            .change(PasswordChange(testPassword, this))
                                        it.setRollbackOnly()
                                    }
                                }

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
                                        passwordEncoder.matches(
                                            this@updatedPassword,
                                            password
                                        )
                                    ).isTrue
                                    assertThat(
                                        passwordEncoder.matches(
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
            fun `test change password with too small password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)

                val testLogin = "change-password-too-small"
                val testPassword = "password-too-small"
                val tooSmallPassword = "1*2"
                assertThat(userTest.id).isNull()

                validator
                    .validateProperty(userTest.copy(login = testLogin), LOGIN_ATTR)
                    .run(::assertThat)
                    .isEmpty()
                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = userTest.password
                    ),
                    PasswordChange.Attributes.CURRENT_PASSWORD_ATTR
                ).run(::assertThat).isEmpty()

                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = tooSmallPassword
                    ),
                    PasswordChange.Attributes.NEW_PASSWORD_ATTR
                ).run(::assertThat).isNotEmpty()

                context.tripleCounts().run {
                    val uuid: UUID = (userTest.copy(
                        login = testLogin,
                        password = testPassword
                    ) to context).signup().getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(second + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password attempt: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                testPassword,
                                second,
                                message = "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(testPassword, second),
                                message = "passwords should match"
                            )

                            tooSmallPassword.run updatedPassword@{
                                assertEquals(testLogin, getCurrentUserLogin())
                                assertTrue(
                                    passwordEncoder.matches(
                                        testPassword, FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
                                            .fetch()
                                            .awaitSingle()[PASSWORD_FIELD]
                                            .toString()
                                            .also { i("password retrieved after user update: $it") }
                                    ).apply { "passwords matches : ${toString()}".run(::i) },
                                    message = "password should be updated"
                                )

                                assertThrows<ValidationException> {
                                    passwordService.change(
                                        PasswordChange(testPassword, this)
                                    )
                                }

                                client
                                    .post()
                                    .uri(API_CHANGE_PASSWORD_PATH)
                                    .contentType(APPLICATION_PROBLEM_JSON)
                                    .header(HttpHeaders.ACCEPT_LANGUAGE, ENGLISH.language)
                                    .bodyValue(PasswordChange(testPassword, this))
                                    .exchange()
                                    .expectStatus()
                                    .isBadRequest
                                    .returnResult<ProblemDetail>()
                                    .responseBodyContent!!
                                    .apply { assertThat(isNotEmpty()).isTrue }
                                    .responseToString()
                                    .run(::assertThat)
                                    .contains("size must be between ${Signup.Constraints.PASSWORD_MIN} and ${Signup.Constraints.PASSWORD_MAX}")

                                context.findOne<User>(testLogin).getOrNull()!!.run {
                                    assertThat(
                                        passwordEncoder.matches(
                                            this@updatedPassword,
                                            password
                                        )
                                    ).isFalse
                                    assertThat(
                                        passwordEncoder.matches(
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
            fun `test change password with too long password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)

                val testLogin = "change-password-too-long"
                val testPassword = "password-too-long"
                val tooLongPassword = "1Change-password-too-long*"
                assertThat(userTest.id).isNull()

                validator
                    .validateProperty(userTest.copy(login = testLogin), LOGIN_ATTR)
                    .run(::assertThat)
                    .isEmpty()
                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = userTest.password
                    ),
                    PasswordChange.Attributes.CURRENT_PASSWORD_ATTR
                ).run(::assertThat).isEmpty()

                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = tooLongPassword
                    ),
                    PasswordChange.Attributes.NEW_PASSWORD_ATTR
                ).run(::assertThat).isNotEmpty()

                context.tripleCounts().run {
                    val uuid: UUID = (userTest.copy(
                        login = testLogin,
                        password = testPassword
                    ) to context).signup().getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(second + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password attempt: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                testPassword,
                                second,
                                message = "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(testPassword, second),
                                message = "passwords should match"
                            )

                            tooLongPassword.run updatedPassword@{
                                assertEquals(testLogin, getCurrentUserLogin())
                                assertTrue(
                                    passwordEncoder.matches(
                                        testPassword, FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
                                            .fetch()
                                            .awaitSingle()[PASSWORD_FIELD]
                                            .toString()
                                            .also { i("password retrieved after user update: $it") }
                                    ).apply { "passwords matches : ${toString()}".run(::i) },
                                    message = "password should be updated"
                                )

                                assertThrows<ValidationException> {
                                    passwordService.change(
                                        PasswordChange(testPassword, this)
                                    )
                                }

                                client
                                    .post()
                                    .uri(API_CHANGE_PASSWORD_PATH)
                                    .contentType(APPLICATION_PROBLEM_JSON)
                                    .header(HttpHeaders.ACCEPT_LANGUAGE, ENGLISH.language)
                                    .bodyValue(PasswordChange(testPassword, this))
                                    .exchange()
                                    .expectStatus()
                                    .isBadRequest
                                    .returnResult<ProblemDetail>()
                                    .responseBodyContent!!
                                    .apply { assertThat(isNotEmpty()).isTrue }
                                    .responseToString()
                                    .run(::assertThat)
                                    .contains("size must be between ${Signup.Constraints.PASSWORD_MIN} and ${Signup.Constraints.PASSWORD_MAX}")

                                context.findOne<User>(testLogin).getOrNull()!!.run {
                                    assertThat(
                                        passwordEncoder.matches(
                                            this@updatedPassword,
                                            password
                                        )
                                    ).isFalse
                                    assertThat(
                                        passwordEncoder.matches(
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
            @WithMockUser("change-password-empty")
            fun `test change password with empty password`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)


                val testLogin = "change-password-empty"
                val testPassword = "changePasswordEmpty!"
                val emptyPassword = EMPTY_STRING

                assertThat(userTest.id).isNull()


                validator
                    .validateProperty(userTest.copy(login = testLogin), LOGIN_ATTR)
                    .run(::assertThat)
                    .isEmpty()

                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = userTest.password
                    ),
                    PasswordChange.Attributes.CURRENT_PASSWORD_ATTR
                ).run(::assertThat).isEmpty()

                validator.validateProperty(
                    PasswordChange(
                        currentPassword = testPassword,
                        newPassword = emptyPassword
                    ),
                    PasswordChange.Attributes.NEW_PASSWORD_ATTR
                ).run(::assertThat).isNotEmpty()

                context.tripleCounts().run {
                    val uuid: UUID = (userTest.copy(
                        login = testLogin,
                        password = testPassword
                    ) to context).signup().getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(second + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            (this[User.Relations.Fields.ID_FIELD].toString()
                                .run(UUID::fromString) to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password attempt: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                testPassword,
                                second,
                                message = "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(testPassword, second),
                                message = "passwords should match"
                            )

                            emptyPassword.run updatedPassword@{
                                assertEquals(testLogin, getCurrentUserLogin())
                                assertTrue(
                                    passwordEncoder.matches(
                                        testPassword,
                                        FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
                                            .fetch()
                                            .awaitSingle()[PASSWORD_FIELD]
                                            .toString()
                                            .also { i("password retrieved after user update: $it") }
                                    ).apply { "passwords matches : ${toString()}".run(::i) },
                                    message = "password should be updated"
                                )

                                assertThrows<ValidationException> {
                                    passwordService.change(
                                        PasswordChange(testPassword, this)
                                    )
                                }

                                client
                                    .post()
                                    .uri(API_CHANGE_PASSWORD_PATH)
                                    .contentType(APPLICATION_PROBLEM_JSON)
                                    .header(HttpHeaders.ACCEPT_LANGUAGE, ENGLISH.language)
                                    .bodyValue(PasswordChange(testPassword, this))
                                    .exchange()
                                    .expectStatus()
                                    .isBadRequest
                                    .returnResult<ProblemDetail>()
                                    .responseBodyContent!!
                                    .apply { assertThat(isNotEmpty()).isTrue }
                                    .responseToString()
                                    .run(::assertThat)
                                    .contains("size must be between ${Signup.Constraints.PASSWORD_MIN} and ${Signup.Constraints.PASSWORD_MAX}")

                                context.findOne<User>(testLogin).getOrNull()!!.run {
                                    assertThat(
                                        passwordEncoder.matches(
                                            this@updatedPassword,
                                            password
                                        )
                                    ).isFalse
                                    assertThat(
                                        passwordEncoder.matches(
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
            fun `test initiate reset password with valid email on well signed up user`()
                    : Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest.copy(
                        login = USER,
                        password = Constants.PASSWORD
                    ) to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            @Suppress("RemoveRedundantQualifierName")
                            (this[User.Relations.Fields.ID_FIELD].toString().run(UUID::fromString)
                                    to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                Constants.PASSWORD,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder
                                    .matches(Constants.PASSWORD, second),
                                message = "password should not be different"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    Constants.PASSWORD, FIND_ALL_USERS
                                        .trimIndent()
                                        .run(db::sql)
                                        .fetch()
                                        .awaitSingle()[PASSWORD_FIELD]
                                        .toString()
                                        .also { i("password retrieved after user signup: $it") }
                                ).apply { "passwords matches : ${toString()}".run(::i) },
                                message = "password should be encoded"
                            )
                            // Given a user well signed up
                            assertThat(context.countUserResets()).isEqualTo(0)
                            client.post()
                                .uri(API_RESET_PASSWORD_INIT_PATH)
                                .contentType(APPLICATION_PROBLEM_JSON)
                                .bodyValue(userTest.email)
                                .exchange()
                                .expectStatus()
                                .isOk
                                .returnResult<ProblemDetail>()
                                .responseBodyContent!!
                                .apply(::assertThat)
                                .isEmpty()

                            assertThat(context.countUserResets()).isEqualTo(1)

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingle().run {
                                    IS_ACTIVE_FIELD
                                        .run(::get)
                                        .toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat)
                                        .asBoolean()
                                        .isTrue
                                    RESET_KEY_FIELD
                                        .run(::get)
                                        .toString()
                                        .run(::assertThat)
                                        .isNotBlank()
                                }
                        }
                }
            }

            @Test
            fun `test request password reset with uppercased email`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)

                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest.copy(
                        login = USER,
                        password = Constants.PASSWORD
                    ) to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }
                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            @Suppress("RemoveRedundantQualifierName")
                            (this[User.Relations.Fields.ID_FIELD].toString().run(UUID::fromString)
                                    to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                Constants.PASSWORD,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder
                                    .matches(Constants.PASSWORD, second),
                                message = "password should not be different"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    Constants.PASSWORD, FIND_ALL_USERS
                                        .trimIndent()
                                        .run(db::sql)
                                        .fetch()
                                        .awaitSingle()[PASSWORD_FIELD]
                                        .toString()
                                        .also { i("password retrieved after user signup: $it") }
                                ).apply { "passwords matches : ${toString()}".run(::i) },
                                message = "password should be encoded"
                            )
                            // Given a user well signed up
                            assertThat(context.countUserResets()).isEqualTo(0)
                            // When user initiates a password reset
                            client.post()
                                .uri(API_RESET_PASSWORD_INIT_PATH)
                                .contentType(APPLICATION_PROBLEM_JSON)
                                .bodyValue(userTest.email.uppercase())
                                .exchange()
                                .expectStatus()
                                .isOk
                                .returnResult<ProblemDetail>()
                                .responseBodyContent!!
                                .apply(::assertThat)
                                .isEmpty()
                            // Then
                            assertThat(context.countUserResets()).isEqualTo(1)
                            // And
                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingle().run {
                                    IS_ACTIVE_FIELD.run(::get).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isTrue
                                    RESET_KEY_FIELD.run(::get).toString()
                                        .run(::assertThat).isNotBlank()
                                }
                        }
                }
            }

            @Test
            fun `test request password reset against unexisting email`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = User(
                    login = signupTest.login,
                    password = signupTest.password,
                    email = signupTest.email,
                    langKey = FRENCH.language
                )
                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            @Suppress("RemoveRedundantQualifierName")
                            (this[User.Relations.Fields.ID_FIELD].toString().run(UUID::fromString)
                                    to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                signupTest.password,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password,
                                    second
                                ),
                                message = "password should not be different"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password, FIND_ALL_USERS
                                        .trimIndent()
                                        .run(db::sql)
                                        .fetch()
                                        .awaitSingle()[PASSWORD_FIELD]
                                        .toString()
                                        .also { i("password retrieved after user signup: $it") }
                                ).apply { "passwords matches : ${toString()}".run(::i) },
                                message = "password should be encoded"
                            )
                            // Given a user well signed up
                            assertThat(context.countUserResets()).isEqualTo(0)
                            // When user initiates a password reset
                            client.post()
                                .uri(API_RESET_PASSWORD_INIT_PATH)
                                .contentType(APPLICATION_PROBLEM_JSON)
                                .bodyValue("user.email@post.com")
                                .exchange()
                                .expectStatus()
                                .is5xxServerError
                                .returnResult<ProblemDetail>()
                                .responseBodyContent!!
                                .logBody()
                                .responseToString()
                                .apply(::assertThat)
                                .contains("Email not found")

                            assertThat(context.countUserResets()).isEqualTo(0)
                        }
                }
            }

            @Test
            fun `test service finish password reset, reset password scenario`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = USER,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = User(
                        login = signupTest.login,
                        password = signupTest.password,
                        email = signupTest.email,
                        langKey = FRENCH.language
                    )

                    assertThat(userTest.id).isNull()
                    context.tripleCounts().run {
                        val uuid: UUID =
                            (userTest to context).signup()
                                .getOrNull()!!.first
                                .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                        assertThat(context.countUsers()).isEqualTo(first + 1)
                        assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                        assertThat(context.countUserActivation()).isEqualTo(third + 1)

                        FIND_ALL_USERS
                            .trimIndent()
                            .run(db::sql)
                            .fetch().awaitSingle().let { allUsers ->
                                @Suppress("RemoveRedundantQualifierName")
                                (allUsers[User.Relations.Fields.ID_FIELD].toString()
                                    .run(UUID::fromString)
                                        to allUsers[PASSWORD_FIELD].toString())
                            }.let { uuidEncodedPasswordPair ->

                                uuidEncodedPasswordPair
                                    .first
                                    .run { "user.id retrieved before update password: $this" }
                                    .run(::i)

                                assertEquals(
                                    uuid,
                                    uuidEncodedPasswordPair.first,
                                    "user.id should be the same"
                                )

                                assertNotEquals(
                                    signupTest.password,
                                    uuidEncodedPasswordPair.second,
                                    "password should be encoded and not the same"
                                )

                                assertTrue(
                                    passwordEncoder
                                        .matches(
                                            signupTest.password,
                                            uuidEncodedPasswordPair.second
                                        ),
                                    message = "password should not be different"
                                )

                                assertTrue(
                                    passwordEncoder.matches(
                                        signupTest.password, FIND_ALL_USERS
                                            .trimIndent()
                                            .run(db::sql)
                                            .fetch()
                                            .awaitSingle()[PASSWORD_FIELD]
                                            .toString()
                                            .also { i("password retrieved after user signup: $it") }
                                    ).apply { "passwords matches : ${toString()}".run(::i) },
                                    message = "password should be encoded"
                                )

                                // Given a user well signed up user
                                assertThat(context.countUserResets()).isEqualTo(0)
                                val resetKey: String = passwordService
                                    .reset(userTest.email).getOrNull()!!
                                resetKey.apply { i("After request reset password - resetKey: $this") }
                                assertThat(context.countUserResets()).isEqualTo(1)
                                // Let's retrieve the user_reset
                                FIND_ALL_USER_RESETS.trimIndent()
                                    .run(db::sql)
                                    .fetch().awaitSingle().run {
                                        toString().apply(::i)
                                        IS_ACTIVE_FIELD.run(::get)
                                            .toString()
                                            .apply(::parseBoolean)
                                            .run(::assertThat).asBoolean().isTrue
                                        RESET_KEY_FIELD.run(::get)
                                            .toString()
                                            .apply { i("Retrieved key: $this") }
                                            .run(::assertThat).asString()
                                            .isEqualTo(resetKey)
                                        UserReset.Relations.Fields.USER_ID_FIELD.run(::get)
                                            .apply { i("Retrieved user_id: $this") }
                                            .run(::assertThat).asString().isNotBlank()
                                    }

                                val newPassword = "${Constants.PASSWORD}&"

                                passwordService
                                    .finish(newPassword, resetKey).apply {
                                        "Rows updated result : $this".run(::i)
                                        run(::assertThat).isEqualTo(SignupService.TWO_ROWS_UPDATED)
                                    }

                                context.countUserResets().run(::assertThat).isEqualTo(1)

                                FIND_ALL_USER_RESETS
                                    .trimIndent()
                                    .run(db::sql)
                                    .fetch()
                                    .awaitSingleOrNull()!!.run {
                                        IS_ACTIVE_FIELD.run(::get)
                                            .toString()
                                            .apply(::parseBoolean)
                                            .run(::assertThat).asBoolean().isFalse

                                        UserReset.Relations.Fields.CHANGE_DATE_FIELD.run(::get)
                                            .toString()
                                            .run(::assertThat).asString()
                                            .containsAnyOf(
                                                ofInstant(
                                                    now(),
                                                    systemDefault()
                                                ).year.toString(),
                                                ofInstant(
                                                    now(),
                                                    systemDefault()
                                                ).month.toString(),
                                                ofInstant(
                                                    now(),
                                                    systemDefault()
                                                ).dayOfMonth.toString(),
                                                ofInstant(
                                                    now(),
                                                    systemDefault()
                                                ).hour.toString(),
                                            )
                                    }

                                FIND_ALL_USERS
                                    .trimIndent()
                                    .run(db::sql)
                                    .fetch()
                                    .awaitSingleOrNull()!![PASSWORD_FIELD].toString()
                                    .run {
                                        passwordEncoder
                                            .matches(newPassword, this)
                                    }.run(::assertThat).isTrue
                            }
                    }
                }

            @Test
            fun `test finish password reset, reset password scenario`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = User(
                    login = signupTest.login,
                    password = signupTest.password,
                    email = signupTest.email,
                    langKey = FRENCH.language
                )

                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            @Suppress("RemoveRedundantQualifierName")
                            (this[User.Relations.Fields.ID_FIELD].toString().run(UUID::fromString)
                                    to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                signupTest.password,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password,
                                    second
                                ),
                                message = "password should not be different"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password, FIND_ALL_USERS
                                        .trimIndent()
                                        .run(db::sql)
                                        .fetch()
                                        .awaitSingle()[PASSWORD_FIELD]
                                        .toString()
                                        .also { i("password retrieved after user signup: $it") }
                                ).apply { "passwords matches : ${toString()}".run(::i) },
                                message = "password should be encoded"
                            )
                            context.apply {
                                // Given a user well signed up
                                assertThat(countUserResets()).isEqualTo(0)
                            }
                            val resetKey: String = passwordService
                                .reset(userTest.email)
                                .getOrNull()!!.apply {
                                    "reset key : $this".run(::i)
                                    assertThat(context.countUserResets()).isEqualTo(1)
                                }

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingle().run {
                                    get(IS_ACTIVE_FIELD).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isTrue
                                    get(RESET_KEY_FIELD).toString()
                                        .run(::assertThat).asString()
                                        .isEqualTo(resetKey)
                                }

                            // finish reset password
                            val newPassword = "${Constants.PASSWORD}&"

                            client.post()
                                .uri(UserReset.EndPoint.API_RESET_PASSWORD_FINISH_PATH.apply {
                                    "uri : $this".run(::i)
                                })
                                .contentType(APPLICATION_PROBLEM_JSON)
                                .bodyValue(ResetPassword(key = resetKey.trimIndent().apply {
                                    "resetKey on select: $this".run(::i)
                                }, newPassword = newPassword))
                                .exchange()
                                .expectStatus()
                                .isOk
                                .returnResult<ProblemDetail>()
                                .responseBodyContent!!
                                .apply { logBody() }
                                .apply(::assertThat)
                                .isEmpty()


                            context.countUserResets().run(::assertThat).isEqualTo(1)

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingleOrNull()!!.run {
                                    IS_ACTIVE_FIELD.run(::get).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isFalse

                                    UserReset.Relations.Fields.CHANGE_DATE_FIELD.run(::get)
                                        .toString()
                                        .run(::assertThat).asString()
                                        .containsAnyOf(
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).year.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).month.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).dayOfMonth.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).hour.toString(),
                                        )
                                }

                            FIND_ALL_USERS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingleOrNull()!![PASSWORD_FIELD].toString()
                                .run {
                                    passwordEncoder
                                        .matches(newPassword, this)
                                }.run(::assertThat).isTrue
                        }
                }
            }

            @Test
            fun `test finish password reset too small`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = USER,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = User(
                    login = signupTest.login,
                    password = signupTest.password,
                    email = signupTest.email,
                    langKey = FRENCH.language
                )
                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            @Suppress("RemoveRedundantQualifierName")
                            (this[User.Relations.Fields.ID_FIELD].toString().run(UUID::fromString)
                                    to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                signupTest.password,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password,
                                    second
                                ),
                                message = "password should not be different"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password, FIND_ALL_USERS
                                        .trimIndent()
                                        .run(db::sql)
                                        .fetch()
                                        .awaitSingle()[PASSWORD_FIELD]
                                        .toString()
                                        .also { i("password retrieved after user signup: $it") }
                                ).apply { "passwords matches : ${toString()}".run(::i) },
                                message = "password should be encoded"
                            )
                            context.apply {
                                // Given a user well signed up
                                assertThat(countUserResets()).isEqualTo(0)
                            }
                            val resetKey: String = passwordService
                                .reset(userTest.email)
                                .getOrNull()!!.apply {
                                    "reset key : $this".run(::i)
                                    assertThat(context.countUserResets()).isEqualTo(1)
                                }

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingle().run {
                                    get(IS_ACTIVE_FIELD).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isTrue
                                    get(RESET_KEY_FIELD).toString()
                                        .run(::assertThat).asString()
                                        .isEqualTo(resetKey)
                                }

                            //new password
                            val newPassword: String = "!P&".apply {
                                run(::assertThat)
                                    .asString()
                                    .hasSizeLessThan(Signup.Constraints.PASSWORD_MIN)
                            }
                            // finish reset password

                            client.post()
                                .uri(UserReset.EndPoint.API_RESET_PASSWORD_FINISH_PATH
                                    .apply { i("uri : $this") })
                                .contentType(APPLICATION_PROBLEM_JSON)
                                .bodyValue(ResetPassword(key = resetKey.apply {
                                    "resetKey on select: $this".run(::i)
                                }, newPassword = newPassword))
                                .exchange()
                                .expectStatus()
                                .isBadRequest
                                .returnResult<ProblemDetail>()
                                .responseBodyContent!!
                                .apply { logBody() }
                                .responseToString()
                                .run(::assertThat)
                                .asString()
                                .contains("size must be between ${Signup.Constraints.PASSWORD_MIN} and ${Signup.Constraints.PASSWORD_MAX}")


                            context.countUserResets().run(::assertThat).isEqualTo(1)

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingleOrNull()!!.run {
                                    IS_ACTIVE_FIELD.run(::get).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isTrue

                                    UserReset.Relations.Fields.CHANGE_DATE_FIELD.run(::get)
                                        .run(::assertThat).isNull()

                                    UserReset.Relations.Fields.RESET_DATE_FIELD.run(::get)
                                        .toString()
                                        .run(::assertThat).asString()
                                        .containsAnyOf(
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).year.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).month.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).dayOfMonth.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).hour.toString(),
                                        )
                                }

                            FIND_ALL_USERS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingleOrNull()!![PASSWORD_FIELD].toString()
                                .run {
                                    passwordEncoder
                                        .matches(newPassword, this)
                                }.run(::assertThat).isFalse
                        }
                }
            }

            @Test
            fun `test finish password reset with a wrong key`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = User(
                    login = signupTest.login,
                    password = signupTest.password,
                    email = signupTest.email,
                    langKey = FRENCH.language
                )
                assertThat(userTest.id).isNull()
                context.tripleCounts().run {
                    val uuid: UUID = (userTest to context).signup()
                        .getOrNull()!!.first
                        .apply { "user.id from signupDao: ${toString()}".apply(::i) }

                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)

                    FIND_ALL_USERS
                        .trimIndent()
                        .run(db::sql)
                        .fetch().awaitSingle().run {
                            @Suppress("RemoveRedundantQualifierName")
                            (this[User.Relations.Fields.ID_FIELD].toString().run(UUID::fromString)
                                    to this[PASSWORD_FIELD].toString())
                        }.run {
                            "user.id retrieved before update password: $first".apply(::i)
                            assertEquals(uuid, first, "user.id should be the same")
                            assertNotEquals(
                                signupTest.password,
                                second,
                                "password should be encoded and not the same"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password,
                                    second
                                ),
                                message = "password should not be different"
                            )
                            assertTrue(
                                passwordEncoder.matches(
                                    signupTest.password, FIND_ALL_USERS
                                        .trimIndent()
                                        .run(db::sql)
                                        .fetch()
                                        .awaitSingle()[PASSWORD_FIELD]
                                        .toString()
                                        .also { i("password retrieved after user signup: $it") }
                                ).apply { "passwords matches : ${toString()}".run(::i) },
                                message = "password should be encoded"
                            )
                            context.apply {
                                // Given a user well signed up
                                assertThat(countUserResets()).isEqualTo(0)
                            }
                            val resetKey: String = passwordService
                                .reset(userTest.email)
                                .getOrNull()!!.apply {
                                    "reset key : $this".run(::i)
                                    assertThat(context.countUserResets()).isEqualTo(1)
                                }

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingle().run {
                                    get(IS_ACTIVE_FIELD).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isTrue
                                    get(RESET_KEY_FIELD).toString()
                                        .run(::assertThat).asString()
                                        .isEqualTo(resetKey)
                                }

                            //new password
                            val newPassword = "${Constants.PASSWORD}&"
                            val reset = ResetPassword(
                                key = SecurityUtils.generateResetKey,
                                newPassword = newPassword
                            )
                            // finish reset password
                            client.post()
                                .uri(UserReset.EndPoint.API_RESET_PASSWORD_FINISH_PATH)
                                .contentType(APPLICATION_PROBLEM_JSON)
                                .bodyValue(reset)
                                .exchange()
                                .expectStatus()
                                .is5xxServerError
                                .returnResult<ProblemDetail>()
                                .responseBodyContent!!
                                .apply { logBody() }
                                .responseToString()
                                .run(::assertThat)
                                .asString()
                                .contains("No user was found for this reset key")


                            context.countUserResets().run(::assertThat).isEqualTo(1)

                            FIND_ALL_USER_RESETS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingleOrNull()!!.run {
                                    IS_ACTIVE_FIELD.run(::get).toString()
                                        .apply(::parseBoolean)
                                        .run(::assertThat).asBoolean().isTrue

                                    UserReset.Relations.Fields.CHANGE_DATE_FIELD.run(::get)
                                        .run(::assertThat).isNull()

                                    UserReset.Relations.Fields.RESET_DATE_FIELD.run(::get)
                                        .toString()
                                        .run(::assertThat).asString()
                                        .containsAnyOf(
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).year.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).month.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).dayOfMonth.toString(),
                                            ofInstant(
                                                now(),
                                                systemDefault()
                                            ).hour.toString(),
                                        )
                                }

                            FIND_ALL_USERS
                                .trimIndent()
                                .run(db::sql)
                                .fetch()
                                .awaitSingleOrNull()!![PASSWORD_FIELD].toString()
                                .run { passwordEncoder.matches(newPassword, this) }
                                .run(::assertThat).isFalse
                        }
                }
            }
        }


        @Nested
        @TestInstance(PER_CLASS)
        inner class UserSignupTests {

            /**send mail*/
            @Test
            fun `test signupService signup saves user and role_user and user_activation`()
                    : Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }

                context.tripleCounts().run {
                    signupService.signup(signupTest)
                    assertThat(first + 1).isEqualTo(context.countUsers())
                    assertThat(second + 1).isEqualTo(context.countUserAuthority())
                    assertThat(third + 1).isEqualTo(context.countUserActivation())
                }
            }

            /**send mail*/
            @Test
            fun `test signup request with a valid account`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                context.tripleCounts().run {
                    client
                        .post()
                        .uri(Signup.EndPoint.API_SIGNUP_PATH)
                        .contentType(APPLICATION_PROBLEM_JSON)
                        .bodyValue(signupTest)
                        .exchange()
                        .expectStatus()
                        .isCreated
                        .expectBody()
                        .isEmpty
                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertEquals(second + 1, context.countUserAuthority())
                    assertEquals(third + 1, context.countUserActivation())
                }
            }

            @Test//TODO: rewrite test showing the scenario clearly
            fun `test UserRoleDao signup with existing user without user_role`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = name,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = context.user(signupTest)

                    val countUserBefore = context.countUsers()
                    assertThat(countUserBefore).isEqualTo(0)
                    val countUserAuthBefore = context.countUserAuthority()
                    assertThat(countUserAuthBefore).isEqualTo(0)
                    lateinit var result: Either<Throwable, UUID>
                    (userTest to context).save()
                    assertThat(context.countUsers()).isEqualTo(countUserBefore + 1)
                    val userId = db.sql(TestUtils.FIND_USER_BY_LOGIN)
                        .bind(LOGIN_ATTR, userTest.login.lowercase())
                        .fetch()
                        .one()
                        .awaitSingle()[User.Attributes.ID_ATTR]
                        .toString()
                        .run(UUID::fromString)

                    db
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
                        .run(db::sql)
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
            fun `test signup and trying to retrieve the user id from databaseClient object`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = name,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = context.user(signupTest)

                    assertThat(context.countUsers()).isEqualTo(0)
                    (userTest to context).signup().onRight {
                        //Because 36 == UUID.toString().length
                        it.toString()
                            .apply { assertThat(it.first.toString().length).isEqualTo(36) }
                            .apply(::i)
                    }
                    assertThat(context.countUsers()).isEqualTo(1)
                    assertDoesNotThrow {
                        FIND_ALL_USERS
                            .trimIndent()
                            .run(db::sql)
                            .fetch()
                            .all()
                            .collect {
                                it[User.Relations.Fields.ID_FIELD]
                                    .toString()
                                    .run(UUID::fromString)
                            }
                    }
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
                        assertEquals(SignupService.SIGNUP_AVAILABLE, getOrNull()!!)
                    }
                }

            @Test
            fun `signupAvailability should return SIGNUP_NOT_AVAILABLE_AGAINST_LOGIN_AND_EMAIL for all when login and email are not available`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = name,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = context.user(signupTest)
                    assertEquals(0, context.countUsers())
                    (userTest to context).save()
                    assertEquals(1, context.countUsers())
                    (signupTest to context).availability().run {
                        assertEquals(
                            SignupService.SIGNUP_LOGIN_AND_EMAIL_NOT_AVAILABLE,
                            getOrNull()!!
                        )
                    }
                }

            @Test
            fun `signupAvailability should return SIGNUP_EMAIL_NOT_AVAILABLE when only email is not available`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = name,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = context.user(signupTest)

                    assertEquals(0, context.countUsers())
                    (userTest to context).save()
                    assertEquals(1, context.countUsers())
                    (Signup(
                        "testuser",
                        "password",
                        "password",
                        userTest.email
                    ) to context).availability().run {
                        assertEquals(SignupService.SIGNUP_EMAIL_NOT_AVAILABLE, getOrNull()!!)
                    }
                }

            @Test
            fun `signupAvailability should return SIGNUP_LOGIN_NOT_AVAILABLE when only login is not available`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = name,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    val userTest = context.user(signupTest)
                    assertEquals(0, context.countUsers())
                    (userTest to context).save()
                    assertEquals(1, context.countUsers())
                    (Signup(
                        userTest.login,
                        "password",
                        "password",
                        "testuser@example.com"
                    ) to context).availability().run {
                        assertEquals(SignupService.SIGNUP_LOGIN_NOT_AVAILABLE, getOrNull()!!)
                    }
                }

            @Test
            fun `check signup validate implementation`(): Unit {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                setOf(
                    User.Attributes.PASSWORD_ATTR,
                    EMAIL_ATTR,
                    LOGIN_ATTR
                )
                    .map { it to validator.validateProperty(signupTest, it) }
                    .flatMap { (first, second) ->
                        second.map {
                            mapOf<String, String?>(
                                EntityModel.MODEL_FIELD_OBJECTNAME to Signup.objectName,
                                EntityModel.MODEL_FIELD_FIELD to first,
                                EntityModel.MODEL_FIELD_MESSAGE to it.message
                            )
                        }
                    }.toSet()
                    .apply { run(::isEmpty).let(::assertTrue) }
            }

            @Test
            fun `test signup validator with an invalid login`(): Unit =
                mock<ServerWebExchange>()
                    .validator
                    .validateProperty(
                        signup.copy(login = "funky-log(n"),
                        LOGIN_ATTR
                    )
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
            fun `test signup validator with an invalid password`(): Unit {
                val wrongPassword = "123"
                validator
                    .validateProperty(
                        signup.copy(password = wrongPassword),
                        User.Attributes.PASSWORD_ATTR
                    )
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
            fun `test signup request with an invalid url`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }

                val counts = context.countUsers() to context.countUserAuthority()
                assertThat(counts).isEqualTo(0 to 0)
                client.post().uri("${Signup.EndPoint.API_SIGNUP_PATH}/foobar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(signupTest)
                    .exchange()
                    .expectStatus()
                    .isUnauthorized
                    .expectBody()
                    .isEmpty
                    .responseBodyContent!!
                    .logBody()

                assertThat(counts)
                    .isEqualTo(context.countUsers() to context.countUserAuthority())
                context.findOne<User>(signupTest.email).mapLeft {
                    assertThat(it::class.java).isEqualTo(Exception::class.java)
                }.map { assertThat(it.id).isEqualTo(user.id) }
                    .isRight().run(::assertThat).isFalse
            }

            @Test
            fun `test signup request with an invalid login`() = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }

                context.run {
                    tripleCounts().run {
                        client
                            .post()
                            .uri(Signup.EndPoint.API_SIGNUP_PATH)
                            .contentType(APPLICATION_PROBLEM_JSON)
                            .header(HttpHeaders.ACCEPT_LANGUAGE, FRENCH.language)
                            .bodyValue(signupTest.copy(login = "funky-log(n"))
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }

                val countBefore = context.countUsers()
                assertEquals(0, countBefore)
                client
                    .post()
                    .uri(Signup.EndPoint.API_SIGNUP_PATH)
                    .contentType(APPLICATION_PROBLEM_JSON)
                    .bodyValue(signupTest.copy(password = "inv"))
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }

                assertEquals(0, context.countUsers())
                client
                    .post()
                    .uri(Signup.EndPoint.API_SIGNUP_PATH)
                    .contentType(APPLICATION_PROBLEM_JSON)
                    .bodyValue(signupTest.copy(password = "123"))
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
                                    if (s == Constants.VIRGULE && request.last()
                                            .isDigit()
                                    ) append("\n\t")
                                }
                            }.replace("{\"", "\n{\n\t\"")
                            .replace("\"}", "\"\n}")
                            .replace("\",\"", "\",\n\t\"")
                            .contains(
                                validator.validateProperty(
                                    signupTest.copy(password = "123"),
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                context.tripleCounts().run counts@{
                    signupService.signup(signupTest)
                    assertEquals(this@counts.first + 1, context.countUsers())
                    assertEquals(this@counts.second + 1, context.countUserAuthority())
                    assertEquals(third + 1, context.countUserActivation())
                }
                client
                    .post().uri(Signup.EndPoint.API_SIGNUP_PATH)
                    .contentType(APPLICATION_PROBLEM_JSON)
                    .bodyValue(signupTest.copy(login = TestUtils.Data.admin.login))
                    .exchange().expectStatus().isBadRequest
                    .returnResult<ResponseEntity<ProblemDetail>>()
                    .responseBodyContent!!.apply {
                        map { it.toInt().toChar().toString() }
                            .reduce { request, s ->
                                request + buildString {
                                    append(s)
                                    if (s == Constants.VIRGULE && request.last()
                                            .isDigit()
                                    ) append("\n\t")
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                context.tripleCounts().run counts@{
                    signupService.signup(signupTest)
                    assertEquals(this@counts.first + 1, context.countUsers())
                    assertEquals(this@counts.second + 1, context.countUserAuthority())
                    assertEquals(third + 1, context.countUserActivation())
                }
                client
                    .post()
                    .uri(Signup.EndPoint.API_SIGNUP_PATH)
                    .contentType(APPLICATION_PROBLEM_JSON)
                    .bodyValue(signupTest.copy(email = "foo@localhost"))
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
                                    if (s == Constants.VIRGULE && request.last()
                                            .isDigit()
                                    ) append("\n\t")
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
            fun `Verifies the internationalization of validations through REST with an unconform password in French during signup`(): Unit =
                runBlocking {
                    val signupTest = properties.mailbox.noReply.run {
                        Signup(
                            login = name,
                            email = from,
                            password = password,
                            repassword = password
                        )
                    }
                    assertEquals(0, context.countUsers())
                    client
                        .post()
                        .uri(Signup.EndPoint.API_SIGNUP_PATH)
                        .contentType(APPLICATION_PROBLEM_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, FRENCH.language)
                        .bodyValue(signupTest.copy(password = "123"))
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                context.tripleCounts().run {
                    (userTest to context).signup().apply { assertThat(isRight()).isTrue }
                    assertThat(context.countUsers()).isEqualTo(first + 1)
                    assertThat(context.countUserActivation()).isEqualTo(third + 1)
                    assertThat(context.countUserAuthority()).isEqualTo(second + 1)
                }
            }

            @Test
            fun `test find userActivation by key`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                context.tripleCounts().run counts@{
                    (userTest to context).signup()
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
                            context.findUserActivationByKey(second).getOrNull().toString()
                                .run(::i)
                            // BabyStepping to find an implementation and debugging
                            assertDoesNotThrow {
                                first.toString().run(::i)
                                second.run(::i)
                                transactionalOperator.executeAndAwait {
                                    FIND_BY_ACTIVATION_KEY
                                        .run(db::sql)
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                context.tripleCounts().run counts@{
                    (userTest to context).signup().getOrNull()!!.run {
                        assertEquals(
                            "null",
                            FIND_ALL_USERACTIVATION
                                .trimIndent()
                                .run(db::sql)
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
                                .run(db::sql)
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
                    id = UUID.randomUUID(),
                    activationKey = RandomStringUtils.random(
                        UserActivation.ACTIVATION_KEY_SIZE * 2,
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
                            activationKey.length > UserActivation.ACTIVATION_KEY_SIZE
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
                        signupService.activate(activationKey)
                    }
                    signupService.activate(
                        activationKey,
                        mock<ServerWebExchange>()
                    ).toString().run(::i)
                }
            }

            @Test
            fun `test activateService with a valid key`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                context.tripleCounts().run counts@{
                    (userTest to context).signup().getOrNull()!!.run {
                        assertEquals(
                            "null",
                            FIND_ALL_USERACTIVATION
                                .trimIndent()
                                .run(db::sql)
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
                            signupService.activate(second)
                        )
                        assertEquals(this@counts.first + 1, context.countUsers())
                        assertEquals(this@counts.second + 1, context.countUserAuthority())
                        assertEquals(third + 1, context.countUserActivation())
                        assertNotEquals(
                            "null",
                            FIND_ALL_USERACTIVATION
                                .trimIndent()
                                .run(db::sql)
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
                                        if (s == Constants.VIRGULE && request.last()
                                                .isDigit()
                                        ) append("\n\t")
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
                val signupTest = properties.mailbox.noReply.run {
                    Signup(
                        login = name,
                        email = from,
                        password = password,
                        repassword = password
                    )
                }
                val userTest = context.user(signupTest)
                context.tripleCounts().run counts@{
                    (userTest to context).signup().getOrNull()!!.run {
                        assertEquals(
                            "null",
                            FIND_ALL_USERACTIVATION
                                .trimIndent()
                                .run(db::sql)
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
        }

        @Nested
        @TestInstance(PER_CLASS)
        inner class FunctionalTests {
            @Test
            fun `functional test signup and reset password scenario`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(login = name, email = from, password = password, repassword = password)
                }
                (context to client).signupActivationScenario(signupTest)
                // Given a well signed up user
                (context to client).resetPasswordScenario(
                    signupTest.email,
                    "$${signupTest.password}&"
                )
            }

            @Ignore
            @Test
            @WithMockUser(USER)
            fun `functional test signup and change password scenario`(): Unit = runBlocking {
                val signupTest = properties.mailbox.noReply.run {
                    Signup(login = USER, email = from, password = USER, repassword = USER)
                }
                (context to client).signupActivationScenario(signupTest)
                (context to client).changePasswordScenario(
                    PasswordChange(signupTest.password, "$${signupTest.password}&")
                )
            }
        }
    }
}