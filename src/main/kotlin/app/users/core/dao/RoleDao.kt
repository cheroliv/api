package app.users.core.dao

import app.users.core.models.Role
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitSingle
import app.users.core.models.Role.Relations.COUNT
import app.users.core.models.Role.Relations.DELETE_AUTHORITY_BY_ROLE

object RoleDao {
    suspend fun ApplicationContext.countRoles(): Int = COUNT
        .let(getBean<DatabaseClient>()::sql)
        .fetch()
        .awaitSingle()
        .values
        .first()
        .toString()
        .toInt()

    suspend fun ApplicationContext.deleteAuthorityByRole(role: String): Unit =
        DELETE_AUTHORITY_BY_ROLE
            .let(getBean<DatabaseClient>()::sql)
            .bind(Role.Attributes.ID_ATTR, role)
            .await()
}
