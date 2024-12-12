package users.security

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle

object RoleDao {
    suspend fun ApplicationContext.countRoles(): Int =
        "select count(*) from authority;"
            .let(getBean<DatabaseClient>()::sql)
            .fetch()
            .awaitSingle()
            .values
            .first()
            .toString()
            .toInt()
}
