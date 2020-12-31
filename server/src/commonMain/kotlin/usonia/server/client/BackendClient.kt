package usonia.server.client

import usonia.core.client.UsoniaClient
import usonia.core.state.ActionAccess

interface BackendClient: UsoniaClient, ActionAccess
