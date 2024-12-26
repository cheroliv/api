package app.users.profile

import app.utils.Constants
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import app.users.User
import app.users.User.Fields.ID_FIELD
import app.users.profile.UserProfile.Fields.FIRST_NAME_FIELD
import app.users.profile.UserProfile.Fields.IMAGE_URL_FIELD
import app.users.profile.UserProfile.Fields.LAST_NAME_FIELD
import java.util.*

@JvmRecord
data class UserProfile(
    @field:NotNull
    val id: UUID,
    @field:Size(max = 50)
    val firstName: String = Constants.EMPTY_STRING,
    @field:Size(max = 50)
    val lastName: String = Constants.EMPTY_STRING,
    @field:Size(max = 256)
    val imageUrl: String? = null,
) {
    object Fields {
        const val ID = "id"
        const val FIRST_NAME_FIELD = "first_name"
        const val LAST_NAME_FIELD = "last_name"
        const val IMAGE_URL_FIELD = "image_url"
    }

    object Relations {
        const val TABLE_NAME = "`user_profile`"
        const val SQL_SCRIPT = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            "$ID_FIELD"                     UUID PRIMARY KEY,
            "$FIRST_NAME_FIELD"             VARCHAR,
            "$LAST_NAME_FIELD"              VARCHAR,
            "$IMAGE_URL_FIELD"              VARCHAR,
        FOREIGN KEY ("$ID_FIELD") 
        REFERENCES "${User.Relations.TABLE_NAME}" ("$ID_FIELD")
        ON DELETE CASCADE ON UPDATE CASCADE);
"""
        const val INSERT = ""
//                """
//            insert into $TABLE_NAME (
//            ${Fields.LOGIN_FIELD}, ${Fields.EMAIL_FIELD}, ${Fields.PASSWORD_FIELD},
//            ${Fields.FIRST_NAME_FIELD}, ${Fields.LAST_NAME_FIELD}, ${Fields.LANG_KEY_FIELD}, ${Fields.IMAGE_URL_FIELD},
//            ${Fields.ENABLED_FIELD}, ${Fields.ACTIVATION_KEY_FIELD}, ${Fields.RESET_KEY_FIELD}, ${Fields.RESET_DATE_FIELD},
//            ${Fields.CREATED_BY_FIELD}, ${Fields.CREATED_DATE_FIELD}, ${Fields.LAST_MODIFIED_BY_FIELD}, ${Fields.LAST_MODIFIED_DATE_FIELD}, ${Fields.VERSION_FIELD})
//            values (:login, :email, :password, :firstName, :lastName,
//            :langKey, :imageUrl, :enabled, :activationKey, :resetKey, :resetDate,
//            :createdBy, :createdDate, :lastModifiedBy, :lastModifiedDate, :version)
//            """
    }
}