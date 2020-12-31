package usonia.server.client

import usonia.core.state.*

data class ComposedBackendClient(
    private val actionAccess: ActionAccess,
    private val actionPublisher: ActionPublisher,
    private val eventAccess: EventAccess,
    private val eventPublisher: EventPublisher,
    private val configurationAccess: ConfigurationAccess,
):
    BackendClient,
    ActionAccess by actionAccess,
    ActionPublisher by actionPublisher,
    EventAccess by eventAccess,
    EventPublisher by eventPublisher,
    ConfigurationAccess by configurationAccess
