package usonia.core.client

import usonia.core.state.*

class ComposedClient(
    private val actionAccess: ActionAccess,
    private val actionPublisher: ActionPublisher,
    private val eventAccess: EventAccess,
    private val eventPublisher: EventPublisher,
    private val configurationAccess: ConfigurationAccess,
):
    CommonClient,
    ActionAccess by actionAccess,
    ActionPublisher by actionPublisher,
    EventAccess by eventAccess,
    EventPublisher by eventPublisher,
    ConfigurationAccess by configurationAccess

