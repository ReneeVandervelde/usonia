package usonia.rules.locks

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.distinctUntilChanged
import com.inkapplications.coroutines.ongoing.filter
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.server.client.BackendClient

internal class LockOnSecure(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            client.securityState
                .distinctUntilChanged()
                .filter { it == SecurityState.Armed }
                .collectLatest { lockAllDoors(site) }
        }
    }

    private suspend fun lockAllDoors(site: Site) {
        logger.info("Security Armed - Locking All Doors")
        site.findDevicesBy { Action.Lock::class in it.capabilities.actions }
            .map { Action.Lock(it.id, LockState.LOCKED) }
            .run { client.publishAll(this) }
    }
}
