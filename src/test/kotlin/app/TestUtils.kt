@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "PLATFORM_CLASS_MAPPED_TO_KOTLIN"
)

package app

import app.TestUtils.Data.displayInsertUserScript
import app.TestUtils.WithUnauthenticatedMockUser.Factory
import app.users.core.Constants
import app.users.core.Constants.ADMIN
import app.users.core.Constants.DOMAIN_DEV_URL
import app.users.core.Constants.EMPTY_STRING
import app.users.core.Constants.ROLE_ADMIN
import app.users.core.Constants.ROLE_ANONYMOUS
import app.users.core.Constants.ROLE_USER
import app.users.core.Constants.USER
import app.users.core.Constants.VIRGULE
import app.users.core.Loggers.i
import app.users.core.models.EntityModel
import app.users.core.models.EntityModel.Members.withId
import app.users.core.models.User
import app.users.core.models.User.Attributes.EMAIL_ATTR
import app.users.core.models.User.Attributes.ID_ATTR
import app.users.core.models.User.Attributes.LOGIN_ATTR
import app.users.core.models.User.Members.ROLES_MEMBER
import app.users.core.models.User.Relations.Fields.EMAIL_FIELD
import app.users.core.models.User.Relations.Fields.LANG_KEY_FIELD
import app.users.core.models.User.Relations.Fields.LOGIN_FIELD
import app.users.core.models.User.Relations.Fields.PASSWORD_FIELD
import app.users.core.models.User.Relations.Fields.VERSION_FIELD
import app.users.core.models.User.Relations.Fields.TABLE_NAME
import app.users.core.models.Role
import app.users.core.models.Role.Relations
import app.users.core.models.Role.Relations.DELETE_AUTHORITY_BY_ROLE
import app.users.core.models.UserRole
import app.users.core.models.UserRole.Attributes.USER_ID_ATTR
import app.users.core.models.UserRole.Relations.Fields.ROLE_FIELD
import app.users.core.models.UserRole.Relations.Fields.USER_ID_FIELD
import app.users.signup.Signup
import app.users.signup.UserActivation
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Companion.USERACTIVATIONCLASS
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_DATE_FIELD
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_KEY_FIELD
import app.users.signup.UserActivation.Relations.Fields.CREATED_DATE_FIELD
import app.users.signup.UserActivation.Relations.Fields.ID_FIELD
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import kotlinx.coroutines.reactive.collect
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.springframework.beans.factory.getBean
import org.springframework.boot.runApplication
import org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.*
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.io.IOException
import java.lang.Byte
import java.time.LocalDateTime
import java.time.LocalDateTime.parse
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.*
import java.util.UUID.fromString
import java.util.regex.Pattern
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Pair
import kotlin.String
import kotlin.Suppress
import kotlin.Throwable
import kotlin.Throws
import kotlin.Triple
import kotlin.Unit
import kotlin.also
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.apply
import kotlin.getValue
import kotlin.lazy
import kotlin.let
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.repeat
import kotlin.run
import kotlin.test.assertEquals
import kotlin.toString


object TestUtils {
    
    fun main(args: Array<String>): Unit = displayInsertUserScript()


    @Retention(RUNTIME)
    @WithSecurityContext(factory = Factory::class)
    @Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS)
    annotation class WithUnauthenticatedMockUser {
        class Factory : WithSecurityContextFactory<WithUnauthenticatedMockUser?> {
            override fun createSecurityContext(annotation: WithUnauthenticatedMockUser?)
                    : SecurityContext = SecurityContextHolder.createEmptyContext()
        }
    }
    suspend fun ApplicationContext.countRoles(): Int = Relations.COUNT
        .let(getBean<DatabaseClient>()::sql)
        .fetch()
        .awaitSingle()
        .values
        .first()
        .toString()
        .toInt()

    suspend fun ApplicationContext.deleteAuthorityByRole(role: String): Unit =
        DELETE_AUTHORITY_BY_ROLE
            .let(getBean<DatabaseClient>()::sql)
            .bind(Role.Attributes.ID_ATTR, role)
            .await()

    //TODO: test if the activation key size is valid
    fun Pair<String, ApplicationContext>.isActivationKeySizeValid()
            : Set<ConstraintViolation<UserActivation>> = second
        .getBean<Validator>()
        .validateValue(USERACTIVATIONCLASS, ACTIVATION_KEY_ATTR, first)

    const val DELETE_USER = """DELETE FROM "$TABLE_NAME";"""
    const val COUNT = """SELECT COUNT(*) FROM "$TABLE_NAME";"""
    const val FIND_USER_BY_EMAIL = """
            SELECT u."${User.Relations.Fields.ID_FIELD}" 
            FROM "$TABLE_NAME" as u 
            WHERE LOWER(u."$EMAIL_FIELD") = LOWER(:$EMAIL_ATTR)"""

    suspend fun ApplicationContext.countUserActivation(): Int = COUNT_USER_ACTIVATION
        .trimIndent()
        .run(getBean<DatabaseClient>()::sql)
        .fetch()
        .awaitSingle()
        .values
        .first()
        .toString()
        .toInt()

    const val COUNT_USER_ACTIVATION = """SELECT COUNT(*) FROM "${UserActivation.Relations.Fields.TABLE_NAME}";"""

    const val FIND_BY_ACTIVATION_KEY = """
        SELECT * FROM "${UserActivation.Relations.Fields.TABLE_NAME}" as ua
        WHERE ua."$ACTIVATION_KEY_FIELD" = :$ACTIVATION_KEY_ATTR;
        """

    const val FIND_ALL_USERACTIVATION = """SELECT * FROM "${UserActivation.Relations.Fields.TABLE_NAME}";"""

    suspend fun ApplicationContext.deleteAllUserAuthorities(): Unit = DELETE_USER_AUTHORITIES
        .trimIndent()
        .let(getBean<DatabaseClient>()::sql)
        .await()

    suspend fun ApplicationContext.deleteAllUserAuthorityByUserId(id: UUID) =
        DELETE_USER_AUTHORITIES_BY_USER_ID
            .let(getBean<DatabaseClient>()::sql)
            .bind(USER_ID_ATTR, id)
            .await()

    suspend fun ApplicationContext.deleteUserByIdWithAuthorities_(id: UUID) =
        getBean<DatabaseClient>().run {
            DELETE_USER_AUTHORITIES_BY_USER_ID
                .trimIndent()
                .let(::sql)
                .bind(USER_ID_ATTR, id)
                .await()
            DELETE_USER_BY_ID
                .trimIndent()
                .let(::sql)
                .bind(USER_ID_ATTR, id)
                .await()
        }

    const val COUNT_USER_AUTH = """SELECT COUNT(*) FROM "user_authority";"""
    const val DELETE_USER_AUTHORITIES = """DELETE FROM "user_authority";"""

    const val DELETE_USER_AUTHORITIES_BY_USER_ID =
        """delete from "user_authority" as ua where ua."user_id" = :userId;"""

    const val DELETE_USER_AUTHORITIES_BY_LOGIN = """delete from "user_authority" 
                    |where "user_id" = (
                    |select u."id" from "user" as u where u."login" = :login
                    |);"""
    const val DELETE_USER_BY_ID = """DELETE FROM "$TABLE_NAME" AS u WHERE u."${User.Relations.Fields.ID_FIELD}" = :$ID_ATTR;"""

    suspend fun ApplicationContext.countUserAuthority(): Int = COUNT_USER_AUTH
        .trimIndent()
        .let(getBean<DatabaseClient>()::sql)
        .fetch()
        .awaitSingle()
        .values
        .first()
        .toString()
        .toInt()

    val ApplicationContext.queryDeleteAllUserAuthorityByUserLogin
        get() = DELETE_USER_AUTHORITIES_BY_LOGIN
            .trimIndent()

    suspend fun ApplicationContext.deleteAllUserAuthorityByUserLogin(
        login: String
    ) = getBean<DatabaseClient>()
        .sql(queryDeleteAllUserAuthorityByUserLogin)
        .bind(LOGIN_ATTR, login)
        .await()

    @Suppress("RemoveRedundantQualifierName")
    const val FIND_USER_WITH_AUTHS_BY_ID = """
                            SELECT
                                u."${User.Relations.Fields.ID_FIELD}",
                                u."$EMAIL_FIELD",
                                u."$LOGIN_FIELD",
                                u."$PASSWORD_FIELD",
                                u.$LANG_KEY_FIELD,
                                u.$VERSION_FIELD,
                                STRING_AGG(DISTINCT a."${Role.Relations.Fields.ID_FIELD}", ', ') AS $ROLES_MEMBER
                            FROM "${User.Relations.Fields.TABLE_NAME}" as u
                            LEFT JOIN 
                                user_authority ua ON u."${UserRole.Relations.Fields.ID_FIELD}" = ua."$USER_ID_FIELD"
                            LEFT JOIN 
                                authority as a ON UPPER(ua."$ROLE_FIELD") = UPPER(a."${Role.Attributes.ID_ATTR}")
                            WHERE 
                                u."${User.Relations.Fields.ID_FIELD}" = :$ID_ATTR
                            GROUP BY 
                                u."${User.Relations.Fields.ID_FIELD}", u."$LOGIN_FIELD",u."$EMAIL_FIELD";
                        """


    suspend inline fun <reified T : EntityModel<UUID>> ApplicationContext.findOne(id: UUID)
            : Either<Throwable, User> = when (T::class) {
        User::class -> {
            try {
                FIND_USER_WITH_AUTHS_BY_ID
                    .trimIndent()
                    .run(getBean<DatabaseClient>()::sql)
                    .bind(ID_ATTR, id)
                    .fetch()
                    .awaitSingleOrNull()
                    .run {
                        when {
                            this == null -> Exception("Not been able to retrieve account.").left()
                            else -> User(
                                id = fromString(get(User.Relations.Fields.ID_FIELD).toString()),
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
            } catch (e: Throwable) {
                e.left()
            }
        }

        else -> (T::class.simpleName)
            .run { "Unsupported type: $this" }
            .run(::IllegalArgumentException)
            .left()
    }

    const val FIND_USER_BY_LOGIN = """
                SELECT u."${User.Relations.Fields.ID_FIELD}" 
                FROM "$TABLE_NAME" AS u 
                WHERE u."$LOGIN_FIELD" = LOWER(:$LOGIN_ATTR);
                """

    suspend fun ApplicationContext.deleteAllUsersOnly(): Unit = DELETE_USER
        .trimIndent()
        .let(getBean<DatabaseClient>()::sql)
        .await()

    suspend fun ApplicationContext.delete(id: UUID): Unit = DELETE_USER_BY_ID
        .trimIndent()
        .let(getBean<DatabaseClient>()::sql)
        .bind(ID_ATTR, id)
        .await()

    suspend fun ApplicationContext.countUsers(): Int = COUNT
        .trimIndent()
        .let(getBean<DatabaseClient>()::sql)
        .fetch()
        .awaitSingle()
        .values
        .first()
        .toString()
        .toInt()



    object Data {
        val writers = listOf(
            "Karl Marx",
            "Jean-Jacques Rousseau",
            "Victor Hugo",
            "Platon",
            "René Descartes",
            "Socrate",
            "Homère",
            "Paul Verlaine",
            "Claude Roy",
            "Bernard Friot",
            "François Bégaudeau",
            "Frederic Lordon",
            "Antonio Gramsci",
            "Georg Lukacs",
            "Franz Kafka",
            "Arthur Rimbaud",
            "Gérard de Nerval",
            "Paul Verlaine",
            "Rocé",
            "Chrétien de Troyes",
            "François Rabelais",
            "Montesquieu",
            "Georg Hegel",
            "Friedrich Engels",
            "Voltaire",
        )
        const val OFFICIAL_SITE = "https://cccp-education.github.io/"
        const val DEFAULT_IMAGE_URL = "https://placehold.it/50x50"
        val admin: User by lazy { userFactory(ADMIN) }
        val user: User by lazy { userFactory(USER) }
        val users: Set<User> = setOf(admin, user)
        const val DEFAULT_USER_JSON = """{
    "login": "$USER",
    "email": "$USER@$DOMAIN_DEV_URL",
    "password": "$USER"}"""
        val signup: Signup by lazy {
            Signup(
                login = user.login,
                password = user.password,
                email = user.email,
                repassword = user.password
            )
        }

        fun userFactory(login: String): User = User(
            password = login,
            login = login,
            email = "$login@$DOMAIN_DEV_URL",
        )

        fun displayInsertUserScript() {
            "InsertUserScript :\n$INSERT_USERS_SCRIPT".run(::println)
        }

        //TODO : add methode to complete user generation
        const val INSERT_USERS_SCRIPT = """
            -- Fonction pour générer des mots de passe aléatoires
            CREATE OR REPLACE FUNCTION random_password(length INT) RETURNS TEXT AS ${'$'}${'$'}
            DECLARE
                chars TEXT := 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
                pwd TEXT := '';
            BEGIN
                FOR i IN 1..length LOOP
                    pwd := pwd || substring(chars from (floor(random() * length(chars)) + 1) for 1);
                END LOOP;
                RETURN pwd;
            END;
            ${'$'}${'$'} LANGUAGE plpgsql;
            
            -- Insertion de 100 nouveaux utilisateurs avec des mots de passe aléatoires
            INSERT INTO "user" ("login", "password", "email", "lang_key")
            VALUES
                ('user1', random_password(10), 'user1@example.com', 'en'),
                ('user2', random_password(10), 'user2@example.com', 'fr'),
                -- ... (répéter 98 fois en remplaçant les noms d'utilisateur et les emails)
                ('user100', random_password(10), 'user100@example.com', 'es');
            
            -- Attribution du rôle "USER" à tous les nouveaux utilisateurs
            INSERT INTO "user_authority" ("user_id", "role")
            SELECT "id", 'USER'
            FROM "user"
            WHERE "id" IN (
                SELECT "id"
                FROM "user"
                WHERE "login" LIKE 'user%'
            );"""
    }

    val ApplicationContext.PATTERN_LOCALE_2: Pattern
        get() = Pattern.compile("([a-z]{2})-([a-z]{2})")

    val ApplicationContext.PATTERN_LOCALE_3: Pattern
        get() = Pattern.compile("([a-z]{2})-([a-zA-Z]{4})-([a-z]{2})")

    val ApplicationContext.languages
        get() = setOf("en", "fr", "de", "it", "es")

    val ApplicationContext.defaultRoles
        get() = setOf(ROLE_ADMIN, ROLE_USER, ROLE_ANONYMOUS)

    fun ApplicationContext.checkProperty(
        property: String,
        value: String,
        injectedValue: String
    ) = property.apply {
        assertThat(value).isEqualTo(let(environment::getProperty))
        assertThat(injectedValue).isEqualTo(let(environment::getProperty))
    }

//    suspend fun ApplicationContext.findAllUsers()
//            : Either<Throwable, List<User>> = FIND_ALL_USERS
//        .trimIndent()
//        .run(getBean<DatabaseClient>()::sql)
//        .fetch()
//        .all()
//        .collect {
//            it.run {
//                User(
//                    id = get(ID_FIELD).toString().run(UUID::fromString),
//                    login = get(LOGIN_ATTR).toString(),
//                    email = get(EMAIL_ATTR).toString(),
//                    langKey = get(LANG_KEY_ATTR).toString(),
//                    roles = mutableSetOf<Role>().apply {
//                        """
//                        SELECT ua."role"
//                        FROM "user" u
//                        JOIN user_authority ua
//                        ON u.id = ua.user_id
//                        WHERE u.id = :userId;"""
//                            .trimIndent()
//                            .run(getBean<DatabaseClient>()::sql)
//                            .bind("userId", get(ID_FIELD))
//                            .fetch()
//                            .all()
//                            .collect { add(Role(it[Role.Fields.ID_FIELD].toString())) }
//                    }.toSet()
//                )
//            }
//        }

    @Throws(EmptyResultDataAccessException::class)
    suspend fun ApplicationContext.findUserActivationByKey(key: String)
            : Either<Throwable, UserActivation> = try {
        FIND_BY_ACTIVATION_KEY
            .trimIndent()
            .run(getBean<R2dbcEntityTemplate>().databaseClient::sql)
            .bind(ACTIVATION_KEY_ATTR, key)
            .fetch()
            .awaitSingleOrNull()
            .let {
                when (it) {
                    null -> return EmptyResultDataAccessException(1).left()
                    else -> return UserActivation(
                        id = it[ID_FIELD].toString().run(UUID::fromString),
                        activationKey = it[ACTIVATION_KEY_FIELD].toString(),
                        createdDate = parse(it[CREATED_DATE_FIELD].toString())
                            .toInstant(UTC),
                        activationDate = it[ACTIVATION_DATE_FIELD].run {
                            when {
                                this == null || toString().lowercase() == "null" -> null
                                else -> toString().run(LocalDateTime::parse).toInstant(UTC)
                            }
                        },
                    ).right()
                }
            }
    } catch (e: Throwable) {
        e.left()
    }

    suspend fun ApplicationContext.findAuthsByEmail(email: String): Either<Throwable, Set<Role>> =
        try {
            mutableSetOf<Role>().apply {
                """
            SELECT ua."role" 
            FROM "user" u 
            JOIN user_authority ua 
            ON u.id = ua.user_id 
            WHERE u."email" = :$EMAIL_ATTR;"""
                    .trimIndent()
                    .run(getBean<DatabaseClient>()::sql)
                    .bind(EMAIL_ATTR, email)
                    .fetch()
                    .all()
                    .collect { add(Role(it[Role.Relations.Fields.ID_FIELD].toString())) }
            }.toSet().right()
        } catch (e: Throwable) {
            e.left()
        }

    suspend fun ApplicationContext.findAuthsByLogin(login: String): Either<Throwable, Set<Role>> =
        try {
            mutableSetOf<Role>().apply {
                """
            SELECT ua."role" 
            FROM "user" u 
            JOIN user_authority ua 
            ON u.id = ua.user_id 
            WHERE u."login" = :$LOGIN_ATTR;"""
                    .trimIndent()
                    .run(getBean<DatabaseClient>()::sql)
                    .bind(LOGIN_ATTR, login)
                    .fetch()
                    .all()
                    .collect { add(Role(it[Role.Relations.Fields.ID_FIELD].toString())) }
            }.toSet().right()
        } catch (e: Throwable) {
            e.left()
        }

    suspend fun ApplicationContext.findUserById(id: UUID): Either<Throwable, User> = try {
        User().withId(id).copy(password = EMPTY_STRING).run user@{
            findAuthsById(id).getOrNull().run roles@{
                return if (isNullOrEmpty())
                    "Unable to retrieve roles from user by id"
                        .run(::Exception)
                        .left()
                else copy(roles = this@roles).right()
            }
        }
    } catch (e: Throwable) {
        e.left()
    }

    suspend fun ApplicationContext.findAuthsById(userId: UUID): Either<Throwable, Set<Role>> = try {
        mutableSetOf<Role>().apply {
            """
            SELECT ua."role" 
            FROM "user" as u 
            JOIN user_authority as ua 
            ON u.id = ua.user_id 
            WHERE u.id = :userId;"""
                .trimIndent()
                .run(getBean<DatabaseClient>()::sql)
                .bind("userId", userId)
                .fetch()
                .all()
                .collect { add(Role(it[Role.Relations.Fields.ID_FIELD].toString())) }
        }.toSet().right()
    } catch (e: Throwable) {
        e.left()
    }

    suspend fun ApplicationContext.tripleCounts() = Triple(
        countUsers().also {
            assertEquals(
                0,
                it,
                "I expected 0 app.users in database."
            )
        },
        countUserAuthority().also {
            assertEquals(
                0,
                it,
                "I expected 0 userAuthority in database."
            )
        },
        countUserActivation().also {
            assertEquals(
                0,
                it,
                "I expected 0 userActivation in database."
            )
        }
    )

    fun launcher(
        vararg profiles: String, userAuths: Set<Pair<String, String>> = emptySet()
    ): ConfigurableApplicationContext = runApplication<API> {
        /** before launching: configuration */
        /** before launching: configuration */
        setEnvironment(StandardReactiveWebEnvironment().apply {
            setDefaultProfiles(Constants.TEST)
            addActiveProfile(Constants.TEST)
            profiles.toSet().map(::addActiveProfile)
        })
    }.apply {
        /** after launching: verification & post construct */
        (when {
            environment.defaultProfiles.isNotEmpty() -> environment.defaultProfiles.reduce { acc, s -> "$acc, $s" }

            else -> ""
        }).let { "defaultProfiles : $it" }.let(app.users.core.Loggers::i)

        (when {
            environment.activeProfiles.isNotEmpty() -> environment.activeProfiles.reduce { acc, s -> "$acc, $s" }

            else -> ""
        }).let { "activeProfiles : $it" }.let(app.users.core.Loggers::i)

        //TODO: ajouter des app.users avec leurs roles
    }

    fun ByteArray.responseToString(): String = map {
        it.toInt().toChar().toString()
    }.reduce { acc: String, s: String -> acc + s }

    //TODO : change that ugly json formating
    fun ByteArray.logBody(): ByteArray = apply {
        if (isNotEmpty()) map { it.toInt().toChar().toString() }.reduce { request, s ->
            request + buildString {
                append(s)
                if (s == VIRGULE && request.last().isDigit()) append("\n\t")
            }
        }.replace("{\"", "\n{\n\t\"").replace("\"}", "\"\n}").replace("\",\"", "\",\n\t\"")
            .run { i("\nbody:$this") }
    }

    fun ByteArray.logBodyRaw(): ByteArray = apply {
        if (isNotEmpty()) map {
            it.toInt().toChar().toString()
        }.reduce { request, s -> request + s }.run { i(this) }
    }

    private fun createObjectMapper() = ObjectMapper().apply {
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        registerModule(JavaTimeModule())
    }

    /**
     * Convert an object to JSON byte array.
     *
     * @param object the object to convert.
     * @return the JSON byte array.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun convertObjectToJsonBytes(`object`: Any): ByteArray =
        createObjectMapper().writeValueAsBytes(`object`)

    /**
     * Create a byte array with a specific size filled with specified data.
     *
     * @param size the size of the byte array.
     * @param data the data to put in the byte array.
     * @return the JSON byte array.
     */
    fun createByteArray(size: Int, data: String) = ByteArray(size) { Byte.parseByte(data, 2) }

    /**
     * A matcher that tests that the examined string represents the same instant as the reference datetime.
     */
    class ZonedDateTimeMatcher(private val date: ZonedDateTime) :
        TypeSafeDiagnosingMatcher<String>() {

        override fun matchesSafely(item: String, mismatchDescription: Description): Boolean {
            try {
                if (!date.isEqual(ZonedDateTime.parse(item))) {
                    mismatchDescription.appendText("was ").appendValue(item)
                    return false
                }
                return true
            } catch (e: DateTimeParseException) {
                mismatchDescription.appendText("was ").appendValue(item)
                    .appendText(", which could not be parsed as a ZonedDateTime")
                return false
            }
        }

        override fun describeTo(description: Description) {
            description.appendText("a String representing the same Instant as ").appendValue(date)
        }
    }

    /**
     * Creates a matcher that matches when the examined string represents the same instant as the reference datetime.
     * @param date the reference datetime against which the examined string is checked.
     */
    fun sameInstant(date: ZonedDateTime) = ZonedDateTimeMatcher(date)

    /**
     * Verifies the equals/hashcode contract on the domain object.
     */
    fun <T : Any> equalsVerifier(clazz: KClass<T>) {
        clazz.createInstance().apply i@{
            assertThat(toString()).isNotNull
            assertThat(this).isEqualTo(this)
            assertThat(hashCode()).isEqualTo(hashCode())
            // Test with an instance of another class
            assertThat(this).isNotEqualTo(Any())
            assertThat(this).isNotEqualTo(null)
            // Test with an instance of the same class
            clazz.createInstance().apply j@{
                assertThat(this@i).isNotEqualTo(this@j)
                // HashCodes are equals because the objects are not persisted yet
                assertThat(this@i.hashCode()).isEqualTo(this@j.hashCode())
            }
        }
    }

    val token64Zero
        get() = mutableListOf<String>().apply {
            repeat(64) { add(0.toString()) }
        }.reduce { acc, i -> "$acc$i" }
}