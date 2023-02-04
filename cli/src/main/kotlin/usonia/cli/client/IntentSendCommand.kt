package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import usonia.cli.CliComponent
import usonia.foundation.Action
import usonia.foundation.Identifier
import javax.inject.Inject

class IntentSendCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    help = "Sends an intent Action to the server to be broadcast."
) {
    private val target by argument()
    private val action by argument()

    override suspend fun execute() {
        client.publishAction(Action.Intent(
            target = target.let(::Identifier),
            action = action
        ))

        echo("Ok")
    }
}
