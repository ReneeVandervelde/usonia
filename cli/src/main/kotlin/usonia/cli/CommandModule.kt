package usonia.cli

import usonia.cli.client.*
import usonia.cli.server.ServerModule
import usonia.cli.server.ServerRunCommand
import usonia.cli.server.TelegramCommands
import usonia.serialization.SerializationModule

object CommandModule {
    private val json = SerializationModule.json
    private val serverModule = ServerModule(json)
    private val clientModule = ClientModule(json)
    val commands = setOf(
        ServerRunCommand(serverModule),
        LogsCommand(clientModule),
        EventsListenCommand(clientModule),
        IntentSendCommand(clientModule),
        SiteUpdateCommand(clientModule, json),
        FlagSetCommand(clientModule),
        FlagRemoveCommand(clientModule),
        ConfigurationDumpCommand(clientModule),
        TelegramCommands()
    )
}
