package usonia.client

import kotlinx.coroutines.flow.Flow
import usonia.core.client.CommonClient
import usonia.foundation.LogMessage

interface FrontendClient: CommonClient {
    /**
     * Ongoing flow of log statements being recorded in the server.
     */
    val logs: Flow<LogMessage>
}
