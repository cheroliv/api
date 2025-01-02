package app

import app.core.Loggers.startupLog
import app.core.Properties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(Properties::class)
class API {
    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runApplication<API>(*args).startupLog()
    }
}