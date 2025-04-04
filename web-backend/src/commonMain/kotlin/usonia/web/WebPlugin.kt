package usonia.web

import kimchi.logger.KimchiLogger
import usonia.serialization.SerializationModule
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.http.HttpController
import usonia.server.http.WebSocketController
import usonia.web.actions.ActionBridgeHttpPublisher
import usonia.web.actions.ActionHttpPublisher
import usonia.web.config.*
import usonia.web.events.*

class WebPlugin(
    client: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    private val json = SerializationModule.json

    override val httpControllers: List<HttpController> = listOf(
        EventHttpPublisher(client, json, logger),
        EventBridgeHttpPublisher(client, json, logger),
        ActionHttpPublisher(client, json, logger),
        ActionBridgeHttpPublisher(client, json, logger),
        LatestEventController(client, json),
        SiteUpdateController(client, json, logger),
        FlagUpdateController(client, json, logger),
        FlagToggleController(client, json, logger),
        FlagDeleteController(client, json),
        ArmSecurityController(client, json),
        DefaultController,
        AppRouteController,
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
        ConfigSocket(client, json),
        EventSocket(client, json, logger),
        FlagListSocket(client, json),
        EventsByDaySocket(client, json),
        OldestEventSocket(client, json),
        TemperatureHistorySnapshotSocket(client, json, logger),
        LatestEventSocket(client, json, logger),
        EventHistorySocket(client, json, logger),
        SecurityStateSocket(client, json),
        EventCountSocket(client, json, logger),
    )
}
