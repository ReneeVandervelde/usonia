package usonia.state

import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.core.state.FullSecurityAccess

/**
 * Functionality fulfilled by the database client.
 */
interface DatabaseServices:
    EventPublisher,
    EventAccess,
    ConfigurationAccess,
    FullSecurityAccess
