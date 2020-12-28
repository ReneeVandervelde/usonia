package usonia.core.client

import usonia.core.state.*

interface CommonClient:
    ActionPublisher,
    EventAccess,
    EventPublisher,
    ConfigurationAccess

