package usonia.server.daemons

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import regolith.processes.daemon.DaemonFailureHandler
import regolith.processes.daemon.DaemonRunAttempt
import regolith.processes.daemon.FailureSignal
import regolith.processes.daemon.failuresPerMinute
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Throttles failures, and eventually panics if the failure rate is too high.
 */
class ThrottledFailureHandler(
    private val timeWindow: Duration = 20.minutes,
    private val minWindowSize: Int = 5,
    private val maxFailuresPerMinute: Float = 1f,
    private val throttleTime: Duration = 30.seconds,
    private val clock: Clock = Clock.System,
): DaemonFailureHandler {
    override suspend fun onFailure(attempts: List<DaemonRunAttempt>): FailureSignal {
        val window = attempts.filter { it.started > clock.now() - timeWindow }
        return when {
            window.size >= minWindowSize && window.failuresPerMinute > maxFailuresPerMinute -> FailureSignal.Panic
            else -> FailureSignal.Restart.also { delay(throttleTime) }
        }
    }
}
