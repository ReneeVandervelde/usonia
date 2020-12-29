package usonia.core.client

import usonia.core.state.*

interface UsoniaClient:
    ActionPublisher,
    EventAccess,
    EventPublisher,
    ConfigurationAccess

