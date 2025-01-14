@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package app.users.signup

import jakarta.validation.constraints.Size
import org.apache.commons.lang3.RandomStringUtils.random
import app.users.User
import app.users.signup.UserActivation.Attributes.ACTIVATION_DATE_ATTR
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Attributes.CREATED_DATE_ATTR
import app.users.signup.UserActivation.Attributes.ID_ATTR
import app.users.signup.UserActivation.Relations.Fields.TABLE_NAME
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_DATE_FIELD
import app.users.signup.UserActivation.Relations.Fields.ACTIVATION_KEY_FIELD
import app.users.signup.UserActivation.Relations.Fields.CREATED_DATE_FIELD
import app.users.signup.UserActivation.Relations.Fields.ID_FIELD
import java.security.SecureRandom
import java.time.Instant
import java.time.Instant.now
import java.util.*

@JvmRecord
data class UserActivation(
    val id: UUID,
    @field:Size(max = ACTIVATION_KEY_SIZE)
    val activationKey: String = random(
        ACTIVATION_KEY_SIZE,
        0,
        0,
        true,
        true,
        null,
        SecureRandom().apply { 64.run(::ByteArray).run(::nextBytes) }
    ),
    val createdDate: Instant = now(),
    val activationDate: Instant? = null,
) {
    companion object {
        const val ACTIVATION_KEY_SIZE = 20
        @Suppress("SpellCheckingInspection")
        val USERACTIVATIONCLASS = UserActivation::class.java
        @JvmStatic
        val objectName: String = USERACTIVATIONCLASS.simpleName.run {
            replaceFirst(
                first(),
                first().lowercaseChar()
            )
        }
    }


    object Attributes {
        //Class
        const val ID_ATTR = ID_FIELD
        const val ACTIVATION_KEY_ATTR = "activationKey"
        const val CREATED_DATE_ATTR = "createdDate"
        const val ACTIVATION_DATE_ATTR = "activationDate"
    }

    object Relations {
        object Fields {
            //SQL
            @Suppress("MemberVisibilityCanBePrivate")
            const val TABLE_NAME = "user_activation"
            const val ID_FIELD = User.Relations.Fields.ID_FIELD
            const val ACTIVATION_KEY_FIELD = "activation_key"
            const val ACTIVATION_DATE_FIELD = "activation_date"
            const val CREATED_DATE_FIELD = "created_date"
        }
        const val SQL_SCRIPT = """
        CREATE TABLE IF NOT EXISTS "$TABLE_NAME" (
            "$ID_FIELD" UUID PRIMARY KEY,
            "$ACTIVATION_KEY_FIELD" VARCHAR,
            "$CREATED_DATE_FIELD" TIMESTAMP,
            "$ACTIVATION_DATE_FIELD" TIMESTAMP,
            FOREIGN KEY ("$ID_FIELD") 
            REFERENCES "${User.Relations.Fields.TABLE_NAME}" ("$ID_FIELD")
            ON DELETE CASCADE ON UPDATE CASCADE
        );

        CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_activation_key
        ON "$TABLE_NAME" ("$ACTIVATION_KEY_FIELD");

        CREATE INDEX IF NOT EXISTS idx_user_activation_date
        ON "$TABLE_NAME" ("$ACTIVATION_DATE_FIELD");

        CREATE INDEX IF NOT EXISTS idx_user_activation_creation_date
        ON "$TABLE_NAME" ("$CREATED_DATE_FIELD");
        """
        const val INSERT = """
        INSERT INTO "$TABLE_NAME" (
            "$ID_FIELD", "$ACTIVATION_KEY_FIELD", 
            "$CREATED_DATE_FIELD", "$ACTIVATION_DATE_FIELD")
        VALUES (
            :$ID_ATTR, :$ACTIVATION_KEY_ATTR, 
            :$CREATED_DATE_ATTR, :$ACTIVATION_DATE_ATTR);
        """

        const val UPDATE_ACTIVATION_BY_KEY = """
        UPDATE "$TABLE_NAME" 
        SET "$ACTIVATION_DATE_FIELD" = NOW() 
        WHERE "$ACTIVATION_KEY_FIELD" = :$ACTIVATION_KEY_ATTR 
        AND "$ACTIVATION_DATE_FIELD" IS NULL;
        """
    }
}