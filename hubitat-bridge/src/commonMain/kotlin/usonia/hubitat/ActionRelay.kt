package usonia.hubitat

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import usonia.core.Daemon
import usonia.core.state.ActionAccess
import usonia.core.state.ConfigurationAccess
import usonia.foundation.*
import usonia.kotlin.neverEnding
import usonia.serialization.ActionSerializer

/**
 * Forwards Action events to a hubitat bridge.
 */
internal class ActionRelay(
    private val configurationAccess: ConfigurationAccess,
    private val actionAccess: ActionAccess,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    private val client = HttpClient {}

    override suspend fun start() = neverEnding {
        configurationAccess.site.collectLatest { site ->
            actionAccess.actions.collectLatest { action ->
                val device = site.getDevice(action.target)
                publish(site, device, action)
            }
        }
    }

    private suspend fun publish(site: Site, device: Device, action: Action) {
        val bridge = site.findAssociatedBridge(device)
        if (bridge?.service != "hubitat") {
            logger.trace("Ignoring non-hubitat action.")
            return
        }

        bridge.publish(device, action)
    }

    private suspend fun Bridge.publish(device: Device, action: Action) {
        logger.info { "Posting action ${action::class.simpleName} to Bridge <${name}>" }
        val parent = device.parent ?: run {
            logger.error("Impossible, no parent for device. Did the publish filtering change?")
            return
        }

        try {
            client.post(
                host = parameters["host"] ?: run {
                    logger.error("`host` not configured for bridge <${id}>. Skipping action.")
                    return
                },
                port = parameters["port"]?.toIntOrNull() ?: run {
                    logger.error("`port` not configured for bridge <${id}>. Skipping action.")
                    return
                },
                path = parameters["actionsPath"] ?: run {
                    logger.error("`actionsPath` not configured for bridge <${id}>. Skipping action.")
                    return
                },
                body = Json.encodeToString(ActionSerializer, action.withTarget(parent.id)),
            ) {
                contentType(ContentType.parse("application/json"))
                parameter("access_token", parameters["token"])
            }
        } catch (error: Throwable) {
            logger.error("Failed to post action to <${name}>", error)
        }
    }
}
