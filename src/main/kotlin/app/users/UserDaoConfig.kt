package app.users

import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration


@Configuration
class UserDaoConfig(val context:ApplicationContext) : AbstractR2dbcConfiguration() {
    override fun connectionFactory(): ConnectionFactory {
        return context.getBean<ConnectionFactory>()
    }
}
