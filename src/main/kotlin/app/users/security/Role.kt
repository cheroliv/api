package app.users.security

import app.core.database.EntityModel
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import app.users.security.Role.Fields.ID_FIELD

data class Role(
    @field:NotNull
    @field:Size(max = 50)
    override val id: String
) : EntityModel<String>() {
    object Fields {
        const val ID_FIELD = "role"
    }

    object Attributes {
        const val ID_ATTR = "role"
    }

    object Constraints

    object Relations {
        const val TABLE_NAME = "authority"
        const val SQL_SCRIPT = """
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME" (
            "$ID_FIELD" VARCHAR(50) PRIMARY KEY
        );
        
        INSERT INTO authority ("$ID_FIELD")
        VALUES ('ADMIN'), 
               ('USER'), 
               ('ANONYMOUS')
        ON CONFLICT ("$ID_FIELD") DO NOTHING;
        """
        const val COUNT = "SELECT COUNT(*) FROM $TABLE_NAME;"
        const val DELETE_AUTHORITY_BY_ROLE="""delete from "authority" as a where upper(a."$ID_FIELD") = upper(:role)"""

    }
}