package usonia.rules.locks

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.combinePair
import com.inkapplications.coroutines.ongoing.filter
import com.inkapplications.coroutines.ongoing.filterIsInstance
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.core.client.alertAll
import usonia.foundation.*
import usonia.server.client.BackendClient

/**
 * Send an alert when unapproved codes are used.
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
    override suspend fun startDaemon(): Nothing {
        client.events
            .filterIsInstance<Event.Lock>()
            .filter { it.state == LockState.UNLOCKED }
            .filter { it.method == Event.Lock.LockMethod.KEYPAD }
            .combinePair(client.site)
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
}
