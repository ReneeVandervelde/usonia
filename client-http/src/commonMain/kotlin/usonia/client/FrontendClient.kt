package usonia.client

import kotlinx.coroutines.flow.Flow
import usonia.core.client.UsoniaClient
import usonia.foundation.LogMessage

interface FrontendClient: UsoniaClient {
    /**
     * Ongoing flow of log statements being recorded in the server.
     */
    val logs: Flow<LogMessage>
}
