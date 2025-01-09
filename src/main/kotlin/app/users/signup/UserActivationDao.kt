@file:Suppress("MemberVisibilityCanBePrivate")

package app.users.signup

import app.core.Loggers.i
import app.core.database.EntityModel
import app.core.web.HttpUtils.validator
import app.users.signup.UserActivation.Attributes.ACTIVATION_DATE_ATTR
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Attributes.CREATED_DATE_ATTR
import app.users.signup.UserActivation.Attributes.ID_ATTR
import app.users.signup.UserActivation.Relations.COUNT
import app.users.signup.UserActivation.Relations.INSERT
import app.users.signup.UserActivation.Relations.UPDATE_ACTIVATION_BY_KEY
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.*
import org.springframework.web.server.ServerWebExchange
import java.util.*

object UserActivationDao {
    suspend fun ApplicationContext.countUserActivation(): Int = COUNT
        .trimIndent()
        .run(getBean<DatabaseClient>()::sql)
        .fetch()
        .awaitSingle()
        .values
        .first()
        .toString()
        .toInt()

    @Throws(EmptyResultDataAccessException::class)
    suspend fun Pair<UserActivation, ApplicationContext>.save()
            : Either<Throwable, Long> = try {
        INSERT.trimIndent()
            .run(second.getBean<R2dbcEntityTemplate>().databaseClient::sql)
            .bind(ID_ATTR, first.id)
            .bind(ACTIVATION_KEY_ATTR, first.activationKey)
            .bind(CREATED_DATE_ATTR, first.createdDate)
            .bind(
                ACTIVATION_DATE_ATTR,
                first.activationDate
            ).fetch()
            .awaitRowsUpdated()
            .right()
    } catch (e: Throwable) {
        e.left()
    }

    /**
     * If the Right value (the result of the database operation) is not equal to 1,
     * then either the key doesn't exist, or the user is already activated.
     */
    suspend fun ApplicationContext.activateDao(key: String): Either<Throwable, Long> = try {
        UPDATE_ACTIVATION_BY_KEY
            .trimIndent()
            .run(getBean<R2dbcEntityTemplate>().databaseClient::sql)
            .bind(ACTIVATION_KEY_ATTR, key)
            .fetch()
            .awaitRowsUpdated()
            .right()
    } catch (e: Throwable) {
        e.left()
    }

    fun UserActivation.validate(
        exchange: ServerWebExchange
    ): Set<Map<String, String?>> = exchange.validator.run {
        i("Validate UserActivation : ${this@validate}")
        setOf(ACTIVATION_KEY_ATTR)
            .map { it to validateProperty(this@validate, it) }
            .flatMap { (first, second) ->
                second.map {
                    mapOf<String, String?>(
                        EntityModel.MODEL_FIELD_OBJECTNAME to UserActivation.objectName,
                        EntityModel.MODEL_FIELD_FIELD to first,
                        EntityModel.MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
    }
}