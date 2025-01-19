package app

import app.users.core.Constants.CLI
import app.users.core.Constants.CLI_PROPS
import app.users.core.Constants.NORMAL_TERMINATION
import app.users.core.Loggers.i
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile(CLI)
class CLI : CommandLineRunner {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<API>(*args) {
                setAdditionalProfiles(CLI)
                setDefaultProperties(CLI_PROPS)
                //before loading config
            }.run {
                //after loading config
            }
            exitProcess(NORMAL_TERMINATION)
        }
    }

    override fun run(vararg args: String?) = runBlocking {
        "command line interface: $args".run(::i)
        "Bienvenu dans le school:cli:  ".run(::i)
    }
}