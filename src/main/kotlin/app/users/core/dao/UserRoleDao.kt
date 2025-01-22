package app.users.core.dao

import app.users.core.models.UserRole
import app.users.core.models.UserRole.Attributes.ROLE_ATTR
import app.users.core.models.UserRole.Attributes.USER_ID_ATTR
import app.users.core.models.UserRole.Relations.Fields.ID_FIELD
import app.users.core.models.UserRole.Relations.INSERT
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.reactive.collect
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.r2dbc.core.DatabaseClient

object UserRoleDao {
    suspend fun Pair<UserRole, ApplicationContext>.signup(): Either<Throwable, Long> = try {
        INSERT.trimIndent()
            .run(second.getBean<DatabaseClient>()::sql)
            .bind(USER_ID_ATTR, first.userId)
            .bind(ROLE_ATTR, first.role)
            .fetch()
            .one()
            .collect { it[ID_FIELD] }
            .toString()
            .toLong()
            .right()
    } catch (e: Exception) {
        e.left()
    }
}