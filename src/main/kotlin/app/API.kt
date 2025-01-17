package app

import app.users.core.Loggers.startupLog
import app.users.core.Properties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(Properties::class)
class API {
    companion object {
        
        fun main(args: Array<String>): Unit = runApplication<API>(*args).startupLog()
    }
}