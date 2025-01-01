package app.users

import app.Constants.EMPTY_STRING
import app.database.EntityModel
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import app.users.User.Attributes.EMAILORLOGIN
import app.users.User.Attributes.EMAIL_ATTR
import app.users.User.Attributes.ID_ATTR
import app.users.User.Attributes.LANG_KEY_ATTR
import app.users.User.Attributes.LOGIN_ATTR
import app.users.User.Attributes.PASSWORD_ATTR
import app.users.User.Attributes.VERSION_ATTR
import app.users.User.Fields.EMAIL_FIELD
import app.users.User.Fields.ID_FIELD
import app.users.User.Fields.LANG_KEY_FIELD
import app.users.User.Fields.LOGIN_FIELD
import app.users.User.Fields.PASSWORD_FIELD
import app.users.User.Fields.VERSION_FIELD
import app.users.User.Members.ROLES_MEMBER
import app.users.User.Relations.CREATE_TABLES
import app.users.security.Role
import app.users.security.UserRole
import app.users.security.UserRole.Fields.ROLE_FIELD
import app.users.security.UserRole.Fields.USER_ID_FIELD
import app.users.signup.UserActivation
import app.Loggers.i
import java.util.*
import java.util.Locale.ENGLISH

data class User(
    override val id: UUID? = null,
    @field:NotNull
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    val login: String = EMPTY_STRING,
    @field:JsonIgnore
    @field:NotNull
    @field:Size(min = 60, max = 60)
    val password: String = EMPTY_STRING,
    @field:Email
    @field:Size(min = 5, max = 254)
    val email: String = EMPTY_STRING,
    @field:JsonIgnore
    val roles: Set<Role> = emptySet(),
    @field:Size(min = 2, max = 10)
    val langKey: String = ENGLISH.language,
    @field:JsonIgnore
    val version: Long = 0,
) : EntityModel<UUID>() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = CREATE_TABLES.run { "CREATE_TABLES: $this" }.run(::i)

        const val LOGIN_REGEX =
            "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$"

        @JvmStatic
        val objectName: String = User::class
            .java
            .simpleName
            .run {
                replaceFirst(
                    first(),
                    first().lowercaseChar()
                )
            }
    }

    object Constraints {
        const val PASSWORD_MIN: Int = 4
        const val PASSWORD_MAX: Int = 16
        const val IMAGE_URL_DEFAULT = "https://placehold.it/50x50"
        const val PHONE_REGEX = "^(\\+|00)?[1-9]\\d{0,49}\$"
    }

    object Members {
        const val PASSWORD_MEMBER = "password"
        const val ROLES_MEMBER = "roles"
    }

    object Fields {
        const val ID_FIELD = "id"
        const val LOGIN_FIELD = "login"
        const val PASSWORD_FIELD = "password"
        const val EMAIL_FIELD = "email"
        const val LANG_KEY_FIELD = "lang_key"
        const val VERSION_FIELD = "version"
    }

    object Attributes {
        const val ID_ATTR = "id"
        const val LOGIN_ATTR = "login"
        const val PASSWORD_ATTR = "password"
        const val EMAIL_ATTR = "email"
        const val LANG_KEY_ATTR = "langKey"
        const val VERSION_ATTR = "version"
        const val EMAILORLOGIN = "emailOrLogin"
    }

    object Relations {
        @JvmStatic
        val CREATE_TABLES: String
            get() = listOf(
                SQL_SCRIPT,
                Role.Relations.SQL_SCRIPT,
                UserRole.Relations.SQL_SCRIPT,
                UserActivation.Relations.SQL_SCRIPT,
            ).joinToString(
                "",
                transform = String::trimIndent
            ).run(String::trimMargin)

        const val TABLE_NAME = "user"
        const val SQL_SCRIPT = """
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME"(
        "$ID_FIELD"       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        "$LOGIN_FIELD"    TEXT,
        "$PASSWORD_FIELD" TEXT,
        "$EMAIL_FIELD"    TEXT,
        "$LANG_KEY_FIELD" VARCHAR,
        "$VERSION_FIELD"  BIGINT);

        CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_user_login" ON "$TABLE_NAME" ("$LOGIN_FIELD");
        CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_user_email" ON "$TABLE_NAME" ("$EMAIL_FIELD");
        """
        const val FIND_ALL_USERS = """SELECT * FROM "$TABLE_NAME";"""

        const val INSERT = """
                insert into "$TABLE_NAME" (
                    "$LOGIN_FIELD", "$EMAIL_FIELD",
                    "$PASSWORD_FIELD", "$LANG_KEY_FIELD",
                    "$VERSION_FIELD"
                ) values ( 
                :$LOGIN_ATTR, 
                :$EMAIL_ATTR, 
                :$PASSWORD_ATTR, 
                :$LANG_KEY_ATTR, 
                :$VERSION_ATTR);"""

        const val FIND_USER_BY_LOGIN = """
                SELECT u."$ID_FIELD" 
                FROM "$TABLE_NAME" AS u 
                WHERE u."$LOGIN_FIELD" = LOWER(:$LOGIN_ATTR);
                """
        const val FIND_USER_BY_LOGIN_OR_EMAIL = """
                SELECT u."$LOGIN_FIELD" 
                FROM "$TABLE_NAME" AS u 
                WHERE LOWER(u."$EMAIL_FIELD") = LOWER(:$EMAIL_ATTR) OR 
                LOWER(u."$LOGIN_FIELD") = LOWER(:$LOGIN_ATTR);
                """

        const val FIND_USER_BY_ID = """SELECT * FROM "$TABLE_NAME" AS u WHERE u.$ID_FIELD = :id;"""

        const val LOGIN_AND_EMAIL_AVAILABLE_COLUMN = "login_and_email_available"
        const val EMAIL_AVAILABLE_COLUMN = "email_available"
        const val LOGIN_AVAILABLE_COLUMN = "login_available"
        const val SELECT_SIGNUP_AVAILABILITY = """
                SELECT
                    CASE
                        WHEN EXISTS(
                                SELECT 1 FROM "$TABLE_NAME" 
                                WHERE LOWER("$LOGIN_FIELD") = LOWER(:$LOGIN_ATTR)
                             ) OR 
                             EXISTS(
                                SELECT 1 FROM "$TABLE_NAME" 
                                WHERE LOWER("$EMAIL_FIELD") = LOWER(:$EMAIL_ATTR)
                             )
                            THEN FALSE
                        ELSE TRUE
                    END AS login_and_email_available,
                    NOT EXISTS(
                            SELECT 1 FROM "$TABLE_NAME" 
                            WHERE LOWER("$EMAIL_FIELD") = LOWER(:$EMAIL_ATTR)
                        ) AS email_available,
                    NOT EXISTS(
                            SELECT 1 FROM "$TABLE_NAME" 
                            WHERE LOWER("$LOGIN_FIELD") = LOWER(:$LOGIN_ATTR)
                        ) AS login_available;
        """
        const val DELETE_USER_BY_ID = """DELETE FROM "$TABLE_NAME" AS u WHERE u."$ID_FIELD" = :$ID_ATTR;"""
        const val DELETE_USER = """DELETE FROM "$TABLE_NAME";"""
        const val FIND_USER_WITH_AUTHS_BY_EMAILOGIN = """
                            SELECT 
                                u."id",
                                u."$EMAIL_FIELD",
                                u."$LOGIN_FIELD",
                                u."$PASSWORD_FIELD",
                                u.$LANG_KEY_FIELD,
                                u.$VERSION_FIELD,
                                STRING_AGG(DISTINCT a."${Role.Fields.ID_FIELD}", ', ') AS $ROLES_MEMBER
                            FROM "$TABLE_NAME" as u
                            LEFT JOIN 
                                user_authority ua ON u."$ID_FIELD" = ua."$USER_ID_FIELD"
                            LEFT JOIN 
                                authority as a ON UPPER(ua."$ROLE_FIELD") = UPPER(a."${Role.Attributes.ID_ATTR}")
                            WHERE 
                                lower(u."$EMAIL_FIELD") = lower(:$EMAILORLOGIN) 
                                OR 
                                lower(u."$LOGIN_FIELD") = lower(:$EMAILORLOGIN)
                            GROUP BY 
                                u."$ID_FIELD", u."$EMAIL_FIELD", u."$LOGIN_FIELD";
                        """

        const val COUNT = """SELECT COUNT(*) FROM "$TABLE_NAME";"""

        const val FIND_USER_BY_EMAIL = """
            SELECT u."$ID_FIELD" 
            FROM "$TABLE_NAME" as u 
            WHERE LOWER(u."$EMAIL_FIELD") = LOWER(:$EMAIL_ATTR)"""
    }
}