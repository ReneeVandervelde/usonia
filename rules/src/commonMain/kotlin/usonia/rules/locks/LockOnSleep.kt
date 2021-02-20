package usonia.rules.locks

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.LockState
import usonia.foundation.findDevicesBy
import usonia.kotlin.filterTrue
import usonia.kotlin.neverEnding
import usonia.rules.sleepMode
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Lock all lock when entering sleep mode.
 */
internal class LockOnSleep(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start(): Nothing = neverEnding {
        client.site.collectLatest {  site ->
            client.sleepMode
                .distinctUntilChanged()
                .filterTrue()
                .collect {
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
