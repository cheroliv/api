@file:Suppress("MemberVisibilityCanBePrivate")

package app.users.security

import app.users.User
import app.users.security.UserRole.Fields.ROLE_FIELD
import app.users.security.UserRole.Fields.USER_ID_FIELD
import jakarta.validation.constraints.NotNull
import java.util.*

@JvmRecord
data class UserRole(
    val id: Long = -1,
    @field:NotNull
    val userId: UUID,
    @field:NotNull
    val role: String
) {
    object Fields {
        const val ID_FIELD = "id"
        const val USER_ID_FIELD = "user_id"
        const val ROLE_FIELD = Role.Fields.ID_FIELD
    }

    object Attributes {
        const val ID_ATTR = Fields.ID_FIELD
        const val USER_ID_ATTR = "userId"
        const val ROLE_ATTR = ROLE_FIELD
    }

    object Relations {
        const val TABLE_NAME = "user_authority"
        const val SQL_SCRIPT = """
    CREATE SEQUENCE IF NOT EXISTS user_authority_seq
    START WITH 1 INCREMENT BY 1;
    CREATE TABLE IF NOT EXISTS "$TABLE_NAME"(
        "${Fields.ID_FIELD}"  BIGINT DEFAULT nextval('user_authority_seq') PRIMARY KEY,
        "$USER_ID_FIELD"      UUID,
        "$ROLE_FIELD"       VARCHAR,
        FOREIGN KEY ("$USER_ID_FIELD") 
        REFERENCES "${User.Relations.TABLE_NAME}" (${User.Fields.ID_FIELD})
        ON DELETE CASCADE ON UPDATE CASCADE,
        FOREIGN KEY ("$ROLE_FIELD") 
        REFERENCES ${Role.Relations.TABLE_NAME} ("${Role.Fields.ID_FIELD}")
        ON DELETE CASCADE ON UPDATE CASCADE
    );

    CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_authority
    ON "$TABLE_NAME" ("$ROLE_FIELD", "$USER_ID_FIELD");
"""
        const val INSERT = """
            INSERT INTO "$TABLE_NAME" ("$USER_ID_FIELD","${Role.Fields.ID_FIELD}")
            VALUES (:${Attributes.USER_ID_ATTR}, :${Attributes.ROLE_ATTR})
            """
    }
}