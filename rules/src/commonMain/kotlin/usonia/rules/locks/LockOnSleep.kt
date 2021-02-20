package usonia.rules.locks

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.LockState
import usonia.foundation.findDevicesBy
import usonia.kotlin.neverEnding
import usonia.rules.sleepMode
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Lock all lock when entering sleep mode.
 */
class LockOnSleep(
    private val client: BackendClient,
): Daemon {
    override suspend fun start(): Nothing = neverEnding {
        client.site.collectLatest {  site ->
            client.sleepMode
                .filter { enabled -> enabled }
                .collect {
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
