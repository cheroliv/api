package app.users.core.dao

import app.users.core.Constants.ROOT_PACKAGE
import app.users.core.Loggers.i
import app.users.core.models.User.Relations.CREATE_TABLES
import app.users.core.models.User.Relations.testCreateTables
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
        fun main(args: Array<String>) = CREATE_TABLES
            .run { "CREATE_TABLES: $this" }
            .run(::i)
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
                    createTempFile("prefix", "suffix").apply {
                        "$testCreateTables$CREATE_TABLES"
                            .trimIndent()
                            .run(::writeText)
                    }.let(::FileSystemResource)
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
                        .apply { "SET search_path = dev;\n$CREATE_TABLES".run(::writeText) }
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