package usonia.state

import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher

/**
 * Composite of the state services provided by the Mongo Database.
 */
interface MongoState: EventAccess, EventPublisher
