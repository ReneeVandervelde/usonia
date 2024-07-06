package usonia.server.client

import usonia.core.client.UsoniaClient
import usonia.core.state.ActionAccess
import usonia.core.state.FullSecurityAccess

interface BackendClient: UsoniaClient, ActionAccess, FullSecurityAccess
