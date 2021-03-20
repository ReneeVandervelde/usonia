package usonia.rules.locks

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.state.allAway
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.Daemon
import usonia.server.client.BackendClient

internal class LockOnAway(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatest { site ->
            client.events
                .filterIsInstance<Event.Presence>()
                .mapLatest { client.allAway(site.users) }
                .distinctUntilChanged()
                .filterTrue()
                .collectLatest { lockAllDoors(site) }
        }
    }

    private suspend fun lockAllDoors(site: Site) {
        logger.info("Locking All Doors for Away Mode")
        site.findDevicesBy { Action.Lock::class in it.capabilities.actions }
            .map { Action.Lock(it.id, LockState.LOCKED) }
            .run { client.publishAll(this) }
    }
}
