@file:Suppress("MemberVisibilityCanBePrivate")

package app.users.core.models

import app.users.core.models.UserRole.Relations.Fields.ID_FIELD
import app.users.core.models.UserRole.Relations.Fields.USER_ROLE_ID_SEQ_FIELD
import app.users.core.models.UserRole.Relations.Fields.ROLE_FIELD
import app.users.core.models.UserRole.Relations.Fields.TABLE_NAME
import app.users.core.models.UserRole.Relations.Fields.USER_ID_FIELD
import app.users.core.models.UserRole.Relations.Fields.USER_ID_ROLE_IDX_FIELD
import jakarta.validation.constraints.NotNull
import java.util.UUID


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
            const val USER_ROLE_ID_SEQ_FIELD = "user_authority_seq"
            const val USER_ID_ROLE_IDX_FIELD = "idx_user_id_role"
        }

        const val SQL_SCRIPT = """
        
        CREATE SEQUENCE IF NOT EXISTS "$USER_ROLE_ID_SEQ_FIELD"
            START WITH 1 INCREMENT BY 1;        
        
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME"(
            "$ID_FIELD"         BIGINT DEFAULT nextval('$USER_ROLE_ID_SEQ_FIELD') PRIMARY KEY,
            "$USER_ID_FIELD"    UUID NOT NULL,
            "$ROLE_FIELD"       VARCHAR NOT NULL,
            UNIQUE ("$USER_ID_FIELD", "$ROLE_FIELD"),
            FOREIGN KEY ("$USER_ID_FIELD") 
                REFERENCES "${User.Relations.Fields.TABLE_NAME}" (${User.Relations.Fields.ID_FIELD})
                ON DELETE CASCADE ON UPDATE CASCADE,
            FOREIGN KEY ("$ROLE_FIELD")
                REFERENCES ${Role.Relations.Fields.TABLE_NAME} ("${Role.Relations.Fields.ID_FIELD}")
                ON DELETE CASCADE ON UPDATE CASCADE
        );
        
        CREATE INDEX IF NOT EXISTS "$USER_ID_ROLE_IDX_FIELD"
            ON "$TABLE_NAME" ("$USER_ID_FIELD", "$ROLE_FIELD");
        
        """

        const val INSERT = """
        INSERT INTO "$TABLE_NAME" ("$USER_ID_FIELD","${Role.Relations.Fields.ID_FIELD}")
            VALUES (:${Attributes.USER_ID_ATTR}, :${Attributes.ROLE_ATTR});
        """
    }
}