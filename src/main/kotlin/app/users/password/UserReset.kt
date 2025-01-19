package app.users.password

import app.users.core.models.User
import app.users.core.models.User.EndPoint.API_USER
import app.users.password.UserReset.Relations.Fields.CHANGE_DATE_FIELD
import app.users.password.UserReset.Relations.Fields.ID_FIELD
import app.users.password.UserReset.Relations.Fields.RESET_DATE_FIELD
import app.users.password.UserReset.Relations.Fields.RESET_KEY_FIELD
import app.users.password.UserReset.Relations.Fields.TABLE_NAME
import app.users.password.UserReset.Relations.Fields.USER_ID_FIELD
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
    val isActive: Boolean,
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
        }

        const val foo = """"""
        const val SQL_SCRIPT = """
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME"(
            "$ID_FIELD"             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            "$USER_ID_FIELD"        UUID NOT NULL,
            "$RESET_KEY_FIELD"      VARCHAR NOT NULL,
            "$RESET_DATE_FIELD"     TIMESTAMP NOT NULL,
            "$CHANGE_DATE_FIELD"    TIMESTAMP NULL,
            "$VERSION_FIELD"        BIGINT DEFAULT 0,
            FOREIGN KEY ("$USER_ID_FIELD")
                REFERENCES "${User.Relations.Fields.TABLE_NAME}"("${User.Relations.Fields.ID_FIELD}")
                ON DELETE CASCADE ON UPDATE CASCADE
        );
        CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_reset_user_id" ON "$TABLE_NAME" ("$USER_ID_FIELD");
        CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_reset_key" ON "$TABLE_NAME" ("$RESET_KEY_FIELD");
        """
    }
}