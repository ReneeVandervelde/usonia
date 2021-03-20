package usonia.core.client

import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher

interface UsoniaClient:
    ActionPublisher,
    EventAccess,
    EventPublisher,
    ConfigurationAccess

