package usonia.rules.locks

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.distinctUntilChanged
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.LockState
import usonia.foundation.findDevicesBy
import usonia.kotlin.filterTrue
import usonia.rules.sleepMode
import usonia.server.client.BackendClient

/**
 * Lock all lock when entering sleep mode.
 */
internal class LockOnSleep(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            client.sleepMode
                .distinctUntilChanged()
                .filterTrue()
                .collectLatest {
                    logger.info("Locking Doors for Sleep Mode.")
                    site.findDevicesBy { Action.Lock::class in it.capabilities.actions }
                        .map {
                            Action.Lock(
                                target = it.id,
                                state = LockState.LOCKED,
                            )
                        }
                        .run { client.publishAll(this) }
                }
        }
    }
}
