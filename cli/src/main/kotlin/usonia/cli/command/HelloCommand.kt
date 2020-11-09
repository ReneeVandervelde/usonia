package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import javax.inject.Inject

class HelloCommand @Inject constructor(): CliktCommand(
    name = "hello"
) {
    override fun run() {
        echo("Hello World!")
    }
}
