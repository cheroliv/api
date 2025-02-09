package app

import app.users.api.Loggers.startupLog
import app.users.api.Properties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(Properties::class)
class Server {
    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runApplication<Server>(*args) {
            setAdditionalProfiles("ai")
        }.startupLog()
    }
}