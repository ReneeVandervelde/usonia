package usonia.client

import kotlinx.coroutines.flow.Flow
import usonia.core.client.UsoniaClient
import usonia.foundation.LogMessage

interface FrontendClient: UsoniaClient {
    /**
     * Ongoing flow of log statements being recorded in the server.
     */
    val logs: Flow<LogMessage>

    /**
     * Ongoing flow of log statements, starting with a buffer of previous logs.
     *
     * Note that the history of this buffer will still be limited by the
     * server's log buffer capacity.
     *
     * @param limit The maximum number of historical log messages to load.
     */
    fun bufferedLogs(limit: Int = Int.MAX_VALUE): Flow<LogMessage>
}
