package usonia.cli.client

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.inkapplications.coroutines.ongoing.collect
import usonia.cli.ColorWriter

class LogsCommand(
    module: ClientModule,
): ClientCommand(
    clientModule = module,
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
