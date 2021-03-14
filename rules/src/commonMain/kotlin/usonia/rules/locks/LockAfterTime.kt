package usonia.rules.locks

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.kotlin.DefaultScope
import usonia.kotlin.collectLatestOngoing
import usonia.kotlin.collectOngoing
import usonia.server.Daemon
import usonia.server.client.BackendClient
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/**
 * Locks entry-point doors after they've been closed for a given amount of time.
 */
@OptIn(ExperimentalTime::class)
class LockAfterTime(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatestOngoing { site ->
            client.events
                .filterIsInstance<Event.Latch>()
                .filter { it.state == LatchState.CLOSED }
                .map { site.findDevice(it.source) }
                .filterNotNull()
                .filter { it.fixture == Fixture.EntryPoint }
                .collectOngoing { onEntrypointClosed(site, it) }
        }
    }

    private suspend fun onEntrypointClosed(site: Site, device: Device) {
        val locks = device.siblings
            .map { site.findDevice(it) }
            .filterNotNull()
            .filter { Action.Lock::class in it.capabilities.actions }

        if (locks.isEmpty()) {
            logger.debug("Skipping Lock timer for latch with no lock siblings.")
            return
        }

        val cancellation = backgroundScope.async {
            client.events
                .filterIsInstance<Event.Latch>()
                .filter { it.state == LatchState.OPEN }
                .filter { it.source == device.id }
                .first()
        }

        val timer = backgroundScope.async {
            delay(20.minutes)
        }

        val shouldLock = select<Boolean> {
            cancellation.onAwait { false }
            timer.onAwait { true }
        }
        timer.cancel()
        cancellation.cancel()

        if (!shouldLock) {
            logger.trace("Lock timer was cancelled")
            return
        }

        locks.map { Action.Lock(it.id, LockState.LOCKED) }
            .run { client.publishAll(this) }
    }
}
