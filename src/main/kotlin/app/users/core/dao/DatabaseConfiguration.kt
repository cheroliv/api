package app.users.core.dao

import app.users.core.Constants.ROOT_PACKAGE
import app.users.core.Loggers
import app.users.core.Loggers.i
import app.users.core.models.User.Relations.CREATE_TABLES
import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.core.io.FileSystemResource
import org.springframework.data.convert.CustomConversions.StoreConversions
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions.STORE_CONVERTERS
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.TransactionalOperator.create
import java.io.File.createTempFile
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDateTime.ofInstant
import java.time.ZoneOffset.UTC

@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories(ROOT_PACKAGE)
class DatabaseConfiguration(private val context: ApplicationContext) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = CREATE_TABLES.run { "CREATE_TABLES: $this" }.run(Loggers::i)
        val testCreateTables: String = """
SET search_path = test;

DROP TABLE IF EXISTS user_authority;
DROP TABLE IF EXISTS user_activation;
DROP TABLE IF EXISTS user_reset;
DROP TABLE IF EXISTS authority;
DROP TABLE IF EXISTS "user";

DROP SEQUENCE IF EXISTS user_authority_seq;
DROP SEQUENCE IF EXISTS user_reset_seq;

DROP INDEX IF EXISTS uniq_idx_user_login;
DROP INDEX IF EXISTS uniq_idx_user_email;
DROP INDEX IF EXISTS uniq_idx_user_authority;
DROP INDEX IF EXISTS uniq_idx_user_activation_key;
DROP INDEX IF EXISTS idx_user_reset_id;
DROP INDEX IF EXISTS idx_user_reset_date;
DROP INDEX IF EXISTS idx_user_reset_userid_resetdate;
DROP INDEX IF EXISTS idx_user_reset_active;

CREATE TABLE IF NOT EXISTS "user"(
    "id"       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "login"    TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "email"    TEXT NOT NULL,
    "lang_key" VARCHAR DEFAULT 'fr',
    "version"  BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_user_login" ON "user" ("login");
CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_user_email" ON "user" ("email");

CREATE TABLE IF NOT EXISTS "authority" (
    "role" VARCHAR(50) PRIMARY KEY
);

INSERT INTO authority ("role") VALUES ('ADMIN'), ('USER'), ('ANONYMOUS')
    ON CONFLICT ("role") DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS user_authority_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS "user_authority"(
    "id"  BIGINT DEFAULT nextval('user_authority_seq') PRIMARY KEY,
    "user_id"      UUID NOT NULL,
    "role"       VARCHAR NOT NULL,
    FOREIGN KEY ("user_id") REFERENCES "user" (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY ("role") REFERENCES authority ("role")
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_authority
    ON "user_authority" ("role", "user_id");

CREATE TABLE IF NOT EXISTS "user_activation" (
    "id" UUID PRIMARY KEY,
    "activation_key" VARCHAR NOT NULL,
    "created_date" TIMESTAMP NOT NULL,
    "activation_date" TIMESTAMP DEFAULT NULL,
    FOREIGN KEY ("id")
    REFERENCES "user" ("id")
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_activation_key
    ON "user_activation" ("activation_key");

CREATE SEQUENCE IF NOT EXISTS user_reset_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS "user_reset" (
    "id"  BIGINT DEFAULT nextval('user_reset_seq') PRIMARY KEY,
    "user_id"        UUID NOT NULL,
    "reset_key"      VARCHAR NOT NULL,
    "reset_date"     TIMESTAMP NOT NULL,
    "change_date"    TIMESTAMP NULL,
    "is_active"      BOOLEAN NOT NULL,
    "version"        BIGINT DEFAULT 0,
    FOREIGN KEY ("user_id") REFERENCES "user"("id")
        ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE("reset_key")
);

CREATE INDEX IF NOT EXISTS idx_user_reset_id ON "user_reset" ("user_id");
CREATE INDEX IF NOT EXISTS idx_user_reset_active ON "user_reset" ("is_active");
CREATE INDEX IF NOT EXISTS idx_user_reset_date ON "user_reset" ("reset_date");
CREATE INDEX IF NOT EXISTS idx_user_reset_userid_resetdate
    ON "user_reset" ("user_id", "reset_date" DESC);
""".trimIndent()
    }

    //TODO: https://reflectoring.io/spring-bean-lifecycle/
    fun createSystemUser(): Unit = i("Creating system user")

    @Bean
    @Profile("test")
    fun localTestConnectionFactoryInitializer(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer =
        ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(
                ResourceDatabasePopulator(
                    createTempFile("prefix", "suffix")
                        .apply { CREATE_TABLES.run(::writeText) }
                        .let(::FileSystemResource)
                )
            )
        }

    @Bean
    @Profile("dev")
    fun localDevConnectionFactoryInitializer(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer =
        ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(
                ResourceDatabasePopulator(
                    createTempFile("prefix", "suffix")
                        .apply { CREATE_TABLES.run(::writeText) }
                        .let(::FileSystemResource)
                )
            )
        }

    @Bean
    @Profile("cloud-dev")
    fun testContainerConnectionFactoryInitializer(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer =
        ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(
                ResourceDatabasePopulator(
                    createTempFile("prefix", "suffix")
                        .apply { CREATE_TABLES.run(::writeText) }
                        .let(::FileSystemResource)
                )
            )
        }

    @Bean
    @Profile("prod")
    fun connectionFactoryInitializer(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer =
        ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(
                ResourceDatabasePopulator(
                    createTempFile("prefix", "suffix")
                        .apply { CREATE_TABLES.run(::writeText) }
                        .let(::FileSystemResource)
                )
            )
        }

    @Bean
    @Profile("local")
    fun localConnectionFactoryInitializer(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer =
        ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(
                ResourceDatabasePopulator(
                    createTempFile("prefix", "suffix")
                        .apply { CREATE_TABLES.run(::writeText) }
                        .let(::FileSystemResource)
                )
            )
        }

    @Bean
    fun reactiveTransactionManager(
        connectionFactory: ConnectionFactory
    ): ReactiveTransactionManager = R2dbcTransactionManager(connectionFactory)

    @Bean
    fun transactionalOperator(
        reactiveTransactionManager: ReactiveTransactionManager
    ): TransactionalOperator = create(reactiveTransactionManager)

    @WritingConverter
    class InstantWriteConverter : Converter<Instant, LocalDateTime> {
        override fun convert(source: Instant): LocalDateTime? = ofInstant(source, UTC)
    }

    @ReadingConverter
    class InstantReadConverter : Converter<LocalDateTime, Instant> {
        override fun convert(localDateTime: LocalDateTime): Instant = localDateTime.toInstant(UTC)!!
    }

    @Bean
    fun r2dbcCustomConversions(
        @Qualifier("connectionFactory")
        connectionFactory: ConnectionFactory
    ): R2dbcCustomConversions = connectionFactory
        .run(DialectResolver::getDialect)
        .run {
            R2dbcCustomConversions(
                StoreConversions.of(
                    simpleTypeHolder,
                    converters.toMutableList().apply {
                        add(InstantWriteConverter())
                        add(InstantReadConverter())
                        addAll(STORE_CONVERTERS)
                    }
                ), mutableListOf<Any>()
            )
        }

}