package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import usonia.client.UsoniaClient
import usonia.foundation.Action
import usonia.foundation.Uuid
import usonia.state.ConfigurationAccess
import javax.inject.Inject

class ClientIntentSendCommand @Inject constructor(
    private val config: ConfigurationAccess
): CliktCommand(
    name = "client:intent:send",
    help = "Sends an intent Action to the server to be broadcast."
) {
    private val action by argument()
    private val host by option().default("localhost")
    private val port by option().int().default(80)
    private val target by option()

    override fun run() = runBlocking {
        val client = UsoniaClient(
            host = host,
            port = port
        )

        val targetId = target?.let(::Uuid) ?: config.site.first().id

        client.sendAction(Action.Intent(
            target = targetId,
            action = action
        ))
    }
}