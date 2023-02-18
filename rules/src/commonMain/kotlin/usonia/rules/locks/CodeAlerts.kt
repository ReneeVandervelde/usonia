package usonia.rules.locks

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.client.alertAll
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Send an alert when unapproved codes are used.
 *
 * This uses a parameter field on the lock device define allowed codes:
 *
 *     "parameters": {
 *         "ownerCodes": "1,2,3"
 *     }
 *
 * Codes used that are not in the `ownerCodes` list will generate an alert
 * notification when the lock is unlocked.
 * If a lock has no ownership codes defined, no notifications will be sent
 * for any codes.
 */
class CodeAlerts(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start(): Nothing {
        client.events
            .filterIsInstance<Event.Lock>()
            .filter { it.state == LockState.UNLOCKED }
            .filter { it.method == Event.Lock.LockMethod.KEYPAD }
            .combineToPair(client.site)
            .collectLatest { (event, site) -> unlockEvent(event, site) }
    }

    private suspend fun unlockEvent(event: Event.Lock, site: Site) {
        val sourceDevice = site.getDevice(event.source)
        if (sourceDevice.ownerCodes.isEmpty()) {
            logger.debug("Skipping guest code alert for door with no owner codes defined.")
            return
        }
        if (event.code !in sourceDevice.ownerCodes) {
            logger.info("Alerting for non-owner code <${event.code}> for door <${sourceDevice.name}>")
            client.alertAll(
                message = "${sourceDevice.name} was opened with a guest code! (Code: ${event.code})",
                level = Action.Alert.Level.Warning,
                icon = Action.Alert.Icon.Suspicious,
            )
        } else {
            logger.info("Not alerting on owner unlock code")
        }
    }

    private val Device.ownerCodes: List<String> get(){
        return parameters.get("ownerCodes")
            ?.split(',')
            ?.map { it.trim() }
            .orEmpty()
    }
}
