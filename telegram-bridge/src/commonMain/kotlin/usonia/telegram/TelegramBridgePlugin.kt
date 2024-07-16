package usonia.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import regolith.processes.daemon.Daemon
import usonia.serialization.SerializationModule
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.http.HttpController
import usonia.telegram.commands.*
import usonia.telegram.commands.Command
import usonia.telegram.commands.MovieStartCommand
import usonia.telegram.commands.SleepCommand
import usonia.telegram.commands.WakeCommand

class TelegramBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val telegram = TelegramClientProxy()
    private val json = Json(SerializationModule.json) {
        ignoreUnknownKeys = true
    }
    private val baseCommands: Set<Command> = setOf(
        WakeCommand(telegram, client),
        SleepCommand(telegram, client),
        MovieStartCommand(telegram, client),
        MovieEndCommand(telegram, client),
        PauseLightsCommand(telegram, client),
        ResumeLightsCommand(telegram, client),
        AnnounceCommand(telegram, client),
        HomeCommand(telegram, client),
        AwayCommand(telegram, client),
        DisableAlertsCommand(telegram, client),
        EnableAlertsCommand(telegram, client),
        ArmCommand(telegram, client),
        DisarmCommand(telegram, client),
    )
    val allCommands = setOf(
        *baseCommands.toTypedArray(),
        WelcomeCommand(telegram, baseCommands),
    )

    override val daemons: List<Daemon> = listOf(
        TelegramTokenUpdater(
            client,
            telegram,
            logger,
        ),
        TelegramAlerts(
            client = client,
            telegram = telegram,
            logger = logger,
        )
    )
    override val httpControllers: List<HttpController> = listOf(
        TelegramBot(client, telegram, allCommands, json, logger),
    )
}
