package app.users.password

import app.users.api.models.User
import app.users.api.models.User.EndPoint.API_USER
import app.users.password.UserReset.Relations.Fields.ACTIVE_IDX_FIELD
import app.users.password.UserReset.Relations.Fields.CHANGE_DATE_FIELD
import app.users.password.UserReset.Relations.Fields.DATE_IDX_FIELD
import app.users.password.UserReset.Relations.Fields.ID_DATE_IDX_FIELD
import app.users.password.UserReset.Relations.Fields.ID_FIELD
import app.users.password.UserReset.Relations.Fields.IS_ACTIVE_FIELD
import app.users.password.UserReset.Relations.Fields.RESET_DATE_FIELD
import app.users.password.UserReset.Relations.Fields.RESET_KEY_FIELD
import app.users.password.UserReset.Relations.Fields.TABLE_NAME
import app.users.password.UserReset.Relations.Fields.USER_ID_FIELD
import app.users.password.UserReset.Relations.Fields.USER_ID_IDX_FIELD
import app.users.password.UserReset.Relations.Fields.USER_RESET_SEQ_FIELD
import app.users.password.UserReset.Relations.Fields.VERSION_FIELD
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserReset(
    val id: UUID? = null,
    @field:NotNull
    val userId: UUID,
    @JsonIgnore
    @field:Size(max = 20)
    val resetKey: String,
    @field:NotNull
    val resetDate: Instant,
    val changeDate: Instant? = null,
    @field:NotNull
    val isActive: Boolean = true,
    @field:JsonIgnore
    val version: Long = 0,
) {
    companion object {
        val objectName: String = UserReset::class.java.simpleName.run {
            replaceFirst(first(), first().lowercaseChar())
        }
    }

    object EndPoint {
        const val API_RESET_PASSWORD_INIT = "/reset-password/init"
        const val API_RESET_PASSWORD_INIT_PATH = "$API_USER$API_RESET_PASSWORD_INIT"
        const val API_CHANGE_PASSWORD = "/change-password"
        const val API_CHANGE_PASSWORD_PATH = "$API_USER$API_CHANGE_PASSWORD"
        const val API_RESET_PASSWORD_FINISH = "/reset-password/finish"
        const val API_RESET_PASSWORD_FINISH_PATH = "$API_USER$API_RESET_PASSWORD_FINISH"
    }

    object Attributes {
        const val ID_ATTR = "id"
        const val USER_ID_ATTR = "userId"
        const val RESET_KEY_ATTR = "resetKey"
        const val RESET_DATE_ATTR = "password"
        const val CHANGE_DATE_ATTR = "password"
        const val IS_ACTIVE_ATTR = "isActive"
        const val VERSION_ATTR = "version"
    }

    object Relations {
        object Fields {
            const val TABLE_NAME = "user_reset"
            const val ID_FIELD = "id"
            const val USER_ID_FIELD = "user_id"
            const val RESET_KEY_FIELD = "reset_key"
            const val RESET_DATE_FIELD = "reset_date"
            const val CHANGE_DATE_FIELD = "change_date"
            const val IS_ACTIVE_FIELD = "is_active"
            const val VERSION_FIELD = "version"
            const val USER_RESET_SEQ_FIELD = "user_reset_seq"
            const val USER_ID_IDX_FIELD = "idx_user_reset_id"
            const val ACTIVE_IDX_FIELD = "idx_user_reset_active"
            const val DATE_IDX_FIELD = "idx_user_reset_date"
            const val ID_DATE_IDX_FIELD = "idx_user_reset_userid_resetdate"
        }

        const val SQL_SCRIPT = """
        
        CREATE SEQUENCE IF NOT EXISTS "$USER_RESET_SEQ_FIELD" START WITH 1 INCREMENT BY 1;
        
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME"(
            "$ID_FIELD"             BIGINT DEFAULT nextval('$USER_RESET_SEQ_FIELD') PRIMARY KEY,
            "$USER_ID_FIELD"        UUID NOT NULL,
            "$RESET_KEY_FIELD"      VARCHAR NOT NULL,
            "$RESET_DATE_FIELD"     TIMESTAMP NOT NULL,
            "$CHANGE_DATE_FIELD"    TIMESTAMP NULL,
            "$IS_ACTIVE_FIELD"      BOOLEAN NOT NULL,            
            "$VERSION_FIELD"        BIGINT DEFAULT 0,
            UNIQUE ("$USER_ID_FIELD", "$RESET_DATE_FIELD"),
            UNIQUE ("$RESET_KEY_FIELD"),        
            FOREIGN KEY ("$USER_ID_FIELD")
                REFERENCES "${User.Relations.Fields.TABLE_NAME}"("${User.Relations.Fields.ID_FIELD}")
                ON DELETE CASCADE ON UPDATE CASCADE
        );        
        CREATE INDEX IF NOT EXISTS "$USER_ID_IDX_FIELD" ON "$TABLE_NAME" ("$USER_ID_FIELD");
        CREATE INDEX IF NOT EXISTS "$ACTIVE_IDX_FIELD" ON "$TABLE_NAME" ("$IS_ACTIVE_FIELD");
        CREATE INDEX IF NOT EXISTS "$DATE_IDX_FIELD" ON "$TABLE_NAME" ("$RESET_DATE_FIELD");
        CREATE INDEX IF NOT EXISTS "$ID_DATE_IDX_FIELD"
            ON "$TABLE_NAME" ("$USER_ID_FIELD", "$RESET_DATE_FIELD" DESC);        
        
        """
        const val INSERT = """
        INSERT INTO user_reset (user_id, reset_key, reset_date, is_active, version)
        VALUES (
            (SELECT id FROM "user" WHERE LOWER(email) = LOWER(:email)),
                :resetKey,
                NOW(),
                TRUE,
                0
        );"""
        const  val FIND_BY_KEY = """
        SELECT ur."user_id" FROM "user_reset" AS ur
        WHERE ur."is_active" IS TRUE
        AND ur."reset_key" = :resetKey ;
        """
        const val UPDATE_CHANGE_DATE_IS_ACTIVE = """
                UPDATE "user_reset"
                SET "is_active" = FALSE,
                "change_date" = NOW()
                WHERE "is_active" is TRUE
                AND "reset_key" = :resetKey
                """
    }
}
