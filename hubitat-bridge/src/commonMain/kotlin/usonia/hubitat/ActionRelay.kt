package usonia.hubitat

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import usonia.foundation.*
import usonia.kotlin.IoScope
import usonia.kotlin.neverEnding
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Forwards Action events to a hubitat bridge.
 */
internal class ActionRelay(
    private val client: BackendClient,
    private val json: Json = Json,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    private val httpClient = HttpClient {}

    override suspend fun start() = neverEnding {
        client.site.collectLatest { site ->
            client.actions.collect { action ->
                onAction(site, action)
            }
        }
    }

    private fun onAction(site: Site, action: Action) {
        val device = site.findDevice(action.target) ?: run {
            logger.trace("Ignoring non-device action")
            return
        }
        publish(site, device, action)
    }

    private fun publish(site: Site, device: Device, action: Action) {
        val bridge = site.findAssociatedBridge(device)
        if (bridge?.service != "hubitat") {
            logger.trace("Ignoring non-hubitat action.")
            return
        }

        bridge.publish(device, action)
    }

    private fun Bridge.publish(device: Device, action: Action) {
        logger.info { "Posting action ${action::class.simpleName} to Bridge <${name}>" }
        val parent = device.parent ?: run {
            logger.error("Impossible, no parent for device. Did the publish filtering change?")
            return
        }

        val host = parameters["host"] ?: run {
            logger.error("`host` not configured for bridge <${id}>. Skipping action.")
            return
        }
        val port = parameters["port"]?.toIntOrNull() ?: run {
            logger.error("`port` not configured for bridge <${id}>. Skipping action.")
            return
        }
        val path = parameters["actionsPath"] ?: run {
            logger.error("`actionsPath` not configured for bridge <${id}>. Skipping action.")
            return
        }

        requestScope.launch {
            try {
                httpClient.post(
                    host = host,
                    port = port,
                    path = path,
                    body = json.encodeToString(ActionSerializer, action.withTarget(parent.id)),
                ) {
                    contentType(ContentType.parse("application/json"))
                    parameter("access_token", parameters["token"])
                }
            } catch (error: Throwable) {
                logger.error("Failed to post action to <${name}>", error)
            }
        }
    }
}
