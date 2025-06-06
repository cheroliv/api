package app

import app.users.api.Constants.CLI
import app.users.api.Constants.CLI_PROPS
import app.users.api.Constants.NORMAL_TERMINATION
import app.users.api.Loggers.i
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile(CLI)
class CommandLine : CommandLineRunner {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<Server>(*args) {
                listOf(CLI, "local", "ai")
                    .toTypedArray()
                    .run(::setAdditionalProfiles)
                setDefaultProperties(CLI_PROPS)
                //before loading config
            }.run {
                //after loading config
            }
            exitProcess(NORMAL_TERMINATION)
        }
    }

    override fun run(vararg args: String?) = runBlocking {
        when (args.isEmpty()) {
            true -> "no arguments"
            else -> args.joinToString()
        }.run { "command line interface: $this" }
            .run(::i)
    }
}