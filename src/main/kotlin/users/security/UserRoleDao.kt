package users.security

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.reactive.collect
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitSingle
import users.User
import users.profile.UserProfile
import users.security.UserRole.Fields.ROLE_FIELD
import users.security.UserRole.Fields.USER_ID_FIELD
import java.util.*

@Suppress("unused")
object UserRoleDao {
    suspend fun Pair<users.security.UserRole, ApplicationContext>.signup(): Either<Throwable, Long> = try {
        second.getBean<R2dbcEntityTemplate>()
            .databaseClient.sql(UserProfile.Relations.INSERT)
            .bind(UserRole.Attributes.USER_ID_ATTR, first.userId)
            .bind(UserRole.Attributes.ROLE_ATTR, first.role)
            .fetch()
            .one()
            .collect { it[UserRole.Fields.ID_FIELD] }
            .toString()
            .toLong()
            .right()
    } catch (e: Exception) {
        e.left()
    }

    suspend fun ApplicationContext.countUserAuthority(): Int =
        "SELECT COUNT(*) FROM user_authority;"
            .let(getBean<DatabaseClient>()::sql)
            .fetch()
            .awaitSingle()
            .values
            .first()
            .toString()
            .toInt()

    suspend fun ApplicationContext.deleteAllUserAuthorities(): Unit =
        "DELETE FROM user_authority;"
            .let(getBean<DatabaseClient>()::sql)
            .await()

    suspend fun ApplicationContext.deleteAllUserAuthorityByUserId(id: UUID) =
        "delete from user_authority as ua where ua.user_id = :userId;"
            .let(getBean<DatabaseClient>()::sql)
            .bind("userId", id)
            .await()

    suspend fun ApplicationContext.deleteAuthorityByRole(role: String): Unit =
        """delete from authority as a where upper(a."${Role.Fields.ID_FIELD}") = upper(:role)"""
            .let(getBean<DatabaseClient>()::sql)
            .bind("role", role)
            .await()

    suspend fun ApplicationContext.deleteUserByIdWithAuthorities_(id: UUID) =
        getBean<DatabaseClient>().run {
            "delete from user_authority where user_id = :userId"
                .let(::sql)
                .bind("userId", id)
                .await()
            """delete from "user" as u where u.${User.Fields.ID_FIELD} = :userId"""
                .let(::sql)
                .await()
        }

    val ApplicationContext.queryDeleteAllUserAuthorityByUserLogin
        get() = """delete from user_authority 
                    |where user_id = (
                    |select u.id from "user" as u where u."login" = :login
                    |);""".trimMargin()

    suspend fun ApplicationContext.deleteAllUserAuthorityByUserLogin(
        login: String
    ): Unit = getBean<DatabaseClient>()
        .sql(queryDeleteAllUserAuthorityByUserLogin)
        .bind("login", login)
        .await()


//            val Pair<User, ApplicationContext>.toJson: String
//                get() = second.getBean<ObjectMapper>().writeValueAsString(first)
//
//            suspend fun Pair<User, ApplicationContext>.save(): Either<Throwable, Long> = try {
//                second
//                    .getBean<R2dbcEntityTemplate>()
//                    .databaseClient
//                    .sql(Relations.INSERT)
//                    .bind("login", first.login)
//                    .bind("email", first.email)
//                    .bind("password", first.password)
////                .bind("firstName", first.firstName)
////                .bind("lastName", first.lastName)
//                    .bind("langKey", first.langKey)
////                .bind("imageUrl", first.imageUrl)
//                    .bind("enabled", first.enabled)
////                .bind("activationKey", first.activationKey)
////                .bind("resetKey", first.resetKey)
////                .bind("resetDate", first.resetDate)
////                .bind("createdBy", first.createdBy)
////                .bind("createdDate", first.createdDate)
////                .bind("lastModifiedBy", first.lastModifiedBy)
////                .bind("lastModifiedDate", first.lastModifiedDate)
//                    .bind("version", first.version)
//                    .fetch()
//                    .awaitRowsUpdated()
//                    .right()
//            } catch (e: Throwable) {
//                e.left()
//            }
}
