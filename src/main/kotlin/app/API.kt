package app

import app.ai.translator.AiTranslatorController.AssistantManager
import app.Loggers.startupLog
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackageClasses = [
        API::class,
        app.users.User::class,
        AssistantManager::class,
    ]
)
@EnableConfigurationProperties(Properties::class)
class API {
    companion object {
        @JvmStatic
        fun main(args: Array<String>): Unit = runApplication<API>(*args).startupLog()
    }
}