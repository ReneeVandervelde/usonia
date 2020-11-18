package usonia.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import usonia.cli.command.ClientLogsCommand
import usonia.cli.command.RunCommand
import kotlin.system.exitProcess

class Main: NoOpCliktCommand() {
    init {
        subcommands(
            RunCommand(),
            ClientLogsCommand(),
        )
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
