package app.users.security

import app.users.security.UserRole.Attributes.ROLE_ATTR
import app.users.security.UserRole.Attributes.USER_ID_ATTR
import app.users.security.UserRole.Relations.Fields.ID_FIELD
import app.users.security.UserRole.Relations.INSERT
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.reactive.collect
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate

object UserRoleDao {
    suspend fun Pair<UserRole, ApplicationContext>.signup(): Either<Throwable, Long> = try {
        INSERT.trimIndent()
            .run(second.getBean<R2dbcEntityTemplate>().databaseClient::sql)
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
