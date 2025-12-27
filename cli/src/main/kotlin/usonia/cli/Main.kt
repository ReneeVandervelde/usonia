package usonia.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import kotlin.system.exitProcess

class Main: NoOpCliktCommand() {
    init {
        CommandModule.commands
            .run(::subcommands)
    }
}

fun main(args: Array<String>) {
    try {
        Main().main(args)
        exitProcess(0)
    } catch (error: Throwable) {
        println("Unknown Error: ${error.message}")
        error.printStackTrace()
        exitProcess(1)
    }
}
