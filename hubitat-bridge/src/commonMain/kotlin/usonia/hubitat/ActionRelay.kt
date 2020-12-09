package usonia.hubitat

import io.ktor.client.*
import io.ktor.client.request.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import usonia.core.Daemon
import usonia.foundation.Action
import usonia.foundation.Bridge
import usonia.kotlin.awaitAll
import usonia.kotlin.neverEnding
import usonia.serialization.ActionSerializer
import usonia.state.ActionAccess
import usonia.state.ConfigurationAccess

/**
 * Forwards Action events to a bridge.
 */
internal class ActionRelay(
    private val configurationAccess: ConfigurationAccess,
    private val actionAccess: ActionAccess,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    private val client = HttpClient {}

    override suspend fun start() = neverEnding {
        coroutineScope {
            configurationAccess.site
                .map { it.bridges.filter { it.service == "hubitat" } }
                .collectLatest { bridges ->
                    actionAccess.actions.collect { action ->
                        bridges.awaitAll { bridge ->
                            async { bridge.publish(action) }
                        }
                    }
                }
        }
    }

    private suspend fun Bridge.publish(action: Action) {
        logger.info { "Posting action ${action::class.simpleName} to Bridge <${name}>" }
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
                body = Json.encodeToString(ActionSerializer, action),
            ) {
                parameter("access_token", parameters["token"])
            }
        } catch (error: Throwable) {
            logger.error("Failed to post action to <${name}>", error)
        }
    }
}
