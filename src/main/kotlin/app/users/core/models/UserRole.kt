@file:Suppress("MemberVisibilityCanBePrivate")

package app.users.core.models

import app.users.core.models.UserRole.Relations.Fields.TABLE_NAME
import app.users.core.models.UserRole.Relations.Fields.ROLE_FIELD
import app.users.core.models.UserRole.Relations.Fields.USER_ID_FIELD
import jakarta.validation.constraints.NotNull
import java.util.*


data class UserRole(
    val id: Long = -1,
    @field:NotNull
    val userId: UUID,
    @field:NotNull
    val role: String
) {

    object Attributes {
        const val ID_ATTR = Relations.Fields.ID_FIELD
        const val USER_ID_ATTR = "userId"
        const val ROLE_ATTR = ROLE_FIELD
    }

    object Relations {
        object Fields {
            const val TABLE_NAME = "user_authority"
            const val ID_FIELD = "id"
            const val USER_ID_FIELD = "user_id"
            const val ROLE_FIELD = Role.Relations.Fields.ID_FIELD
        }
        const val SQL_SCRIPT = """
    CREATE SEQUENCE IF NOT EXISTS user_authority_seq
    START WITH 1 INCREMENT BY 1;
    CREATE TABLE IF NOT EXISTS "$TABLE_NAME"(
        "${Fields.ID_FIELD}"  BIGINT DEFAULT nextval('user_authority_seq') PRIMARY KEY,
        "$USER_ID_FIELD"      UUID,
        "$ROLE_FIELD"       VARCHAR,
        FOREIGN KEY ("$USER_ID_FIELD") 
        REFERENCES "${User.Relations.Fields.TABLE_NAME}" (${User.Relations.Fields.ID_FIELD})
        ON DELETE CASCADE ON UPDATE CASCADE,
        FOREIGN KEY ("$ROLE_FIELD") 
        REFERENCES ${Role.Relations.Fields.TABLE_NAME} ("${Role.Relations.Fields.ID_FIELD}")
        ON DELETE CASCADE ON UPDATE CASCADE
    );

    CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_authority
    ON "$TABLE_NAME" ("$ROLE_FIELD", "$USER_ID_FIELD");
"""
        const val INSERT = """
            INSERT INTO "$TABLE_NAME" ("$USER_ID_FIELD","${Role.Relations.Fields.ID_FIELD}")
            VALUES (:${Attributes.USER_ID_ATTR}, :${Attributes.ROLE_ATTR})
            """
    }
}