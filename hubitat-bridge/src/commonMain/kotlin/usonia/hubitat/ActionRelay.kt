package usonia.hubitat

import com.inkapplications.standard.throwCancels
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import usonia.client.ktor.PlatformEngine
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.Daemon
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Forwards Action events to a hubitat bridge.
 */
internal class ActionRelay(
    private val client: BackendClient,
    private val json: Json = Json,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    private val httpClient = HttpClient(PlatformEngine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10.seconds.inWholeMilliseconds
        }
    }

    override suspend fun start(): Nothing {
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
            logger.trace("Ignoring non-hubitat action for device ${device.id}.")
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
        val token = parameters["token"] ?: run {
            logger.error("`token` not configured for bridge <${id}>. Skipping action.")
            return
        }

        requestScope.launch {
            val result = runRetryable(
                strategy = RetryStrategy.Bracket(
                    attempts = 10,
                    timeouts = listOf(100.milliseconds, 300.milliseconds, 1.seconds, 5.seconds),
                ),
                attemptTimeout = 10.seconds,
                onError = { error -> logger.warn("Error publishing action to <$name>", error) }
            ) {
                httpClient.post {
                    url {
                        this.host = host
                        this.port = port
                        this.path(path)
                        this.parameters.append("access_token", token)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(ActionSerializer, action.withTarget(parent.id)))
                }
            }.throwCancels()

            result.onSuccess {
                logger.debug("Posted action to <$name>: $action")
            }

            result.onFailure { error ->
                logger.error("Failed to post action to <$name>: ${action}", error)
            }
        }
    }
}
