@file:Suppress("MemberVisibilityCanBePrivate")

package app.users.signup

import app.users.core.Loggers.i
import app.users.core.models.EntityModel.Companion.MODEL_FIELD_FIELD
import app.users.core.models.EntityModel.Companion.MODEL_FIELD_MESSAGE
import app.users.core.models.EntityModel.Companion.MODEL_FIELD_OBJECTNAME
import app.users.core.models.User
import app.users.core.web.HttpUtils.validator
import app.users.signup.UserActivation.Attributes.ACTIVATION_DATE_ATTR
import app.users.signup.UserActivation.Attributes.ACTIVATION_KEY_ATTR
import app.users.signup.UserActivation.Attributes.CREATED_DATE_ATTR
import app.users.signup.UserActivation.Attributes.ID_ATTR
import app.users.signup.UserActivation.Relations.INSERT
import app.users.signup.UserActivation.Relations.UPDATE_ACTIVATION_BY_KEY
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.r2dbc.core.*
import org.springframework.web.server.ServerWebExchange
import java.util.*

object SignupDao {

    fun Signup.validate(
        exchange: ServerWebExchange
    ): Set<Map<String, String?>> = exchange.validator.run {
        setOf(
            User.Attributes.PASSWORD_ATTR,
            User.Attributes.EMAIL_ATTR,
            User.Attributes.LOGIN_ATTR,
        ).map { it to validateProperty(this@validate, it) }
            .flatMap { (first, second) ->
                second.map {
                    mapOf<String, String?>(
                        MODEL_FIELD_OBJECTNAME to Signup.objectName,
                        MODEL_FIELD_FIELD to first,
                        MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
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
                        MODEL_FIELD_OBJECTNAME to UserActivation.objectName,
                        MODEL_FIELD_FIELD to first,
                        MODEL_FIELD_MESSAGE to it.message
                    )
                }
            }.toSet()
    }

    @Throws(EmptyResultDataAccessException::class)
    suspend fun Pair<UserActivation, ApplicationContext>.save()
            : Either<Throwable, Long> = try {
        INSERT.trimIndent()
            .run(second.getBean<DatabaseClient>()::sql)
            .bind(ID_ATTR, first.id)
            .bind(ACTIVATION_KEY_ATTR, first.activationKey)
            .bind(CREATED_DATE_ATTR, first.createdDate)
            .bind(
                ACTIVATION_DATE_ATTR,
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
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
    suspend fun ApplicationContext.activate(key: String): Either<Throwable, Long> = try {
        UPDATE_ACTIVATION_BY_KEY
            .trimIndent()
            .run(getBean<DatabaseClient>()::sql)
            .bind(ACTIVATION_KEY_ATTR, key)
            .fetch()
            .awaitRowsUpdated()
            .right()
    } catch (e: Throwable) {
        e.left()
    }
}