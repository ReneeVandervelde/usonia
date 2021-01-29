package usonia.cli.client

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.flow.collect
import usonia.cli.CliComponent
import usonia.cli.ColorWriter
import javax.inject.Inject

class ClientLogsCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    name = "logs",
    help = "Listen to log statements being recorded on the server."
) {
    private val buffer by option(
        help = "Number of historical messages to start with"
    ).int().default(0)

    override suspend fun execute() {
        client.bufferedLogs(buffer).collect {
            ColorWriter.formatLine(it.level, it.message).run(::echo)
            it.stackTrace?.run {
                echo("Caused by:")
                echo(this)
            }
        }
    }
}