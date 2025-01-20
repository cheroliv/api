package app.users.core.models

import app.users.core.models.Role.Attributes.ID_ATTR
import app.users.core.models.Role.Relations.Fields.ID_FIELD
import app.users.core.models.Role.Relations.Fields.TABLE_NAME
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class Role(
    @field:NotNull
    @field:Size(max = 50)
    override val id: String
) : EntityModel<String>() {

    object Attributes {
        const val ID_ATTR = "role"
    }

    object Relations {
        object Fields {
            const val TABLE_NAME = "authority"
            const val ID_FIELD = "role"
        }

        const val SQL_SCRIPT = """
        
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME" (
            "$ID_FIELD" VARCHAR(50) PRIMARY KEY
        );
        
        INSERT INTO "$TABLE_NAME" ("$ID_FIELD")
        VALUES ('ADMIN'), 
               ('USER'), 
               ('ANONYMOUS')
        ON CONFLICT ("$ID_FIELD") DO NOTHING;
        
        """
        const val COUNT = """SELECT COUNT(*) FROM "$TABLE_NAME";"""
        const val DELETE_AUTHORITY_BY_ROLE = """
        delete from "$TABLE_NAME" as a 
            where upper(a."$ID_FIELD") = upper(:$ID_ATTR);"""
    }
}