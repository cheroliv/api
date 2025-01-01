package app

import app.Constants.CLI
import app.Constants.CLI_PROPS
import app.Constants.NORMAL_TERMINATION
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import app.workspace.Log.i
import kotlin.system.exitProcess

@Component
@Profile(CLI)
@SpringBootApplication
class CLI : CommandLineRunner {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<CLI>(*args) {
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