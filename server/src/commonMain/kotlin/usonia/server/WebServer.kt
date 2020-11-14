package usonia.server

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

interface WebServer {
    @OptIn(ExperimentalTime::class)
    suspend fun run(
        gracePeriod: Duration = 5.seconds,
        timeout: Duration = 20.seconds
    )
}
