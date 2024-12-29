package usonia.client

import com.inkapplications.coroutines.ongoing.OngoingFlow
import usonia.core.client.UsoniaClient
import usonia.foundation.LogMessage

interface FrontendClient: UsoniaClient {
    /**
     * Ongoing flow of log statements being recorded in the server.
     */
    val logs: OngoingFlow<LogMessage>

    /**
     * Ongoing flow of log statements, starting with a buffer of previous logs.
     *
     * Note that the history of this buffer will still be limited by the
     * server's log buffer capacity.
     *
     * @param limit The maximum number of historical log messages to load.
     */
    fun bufferedLogs(limit: Int = Int.MAX_VALUE): OngoingFlow<LogMessage>
}
