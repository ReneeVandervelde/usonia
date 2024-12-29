package usonia.rules.locks

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.combinePair
import com.inkapplications.coroutines.ongoing.filter
import com.inkapplications.coroutines.ongoing.filterIsInstance
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.foundation.Event
import usonia.foundation.LockState
import usonia.foundation.Site
import usonia.foundation.getDevice
import usonia.server.client.BackendClient

class DisarmOnPrimaryCode(
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
            logger.debug("Skipping disarm operation for door with no owner codes defined.")
            return
        }
        if (event.code in sourceDevice.ownerCodes) {
            client.disarmSecurity()
        }
    }
}
