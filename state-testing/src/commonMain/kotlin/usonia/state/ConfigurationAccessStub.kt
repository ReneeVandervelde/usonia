package usonia.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import usonia.foundation.Device
import usonia.foundation.ParameterBag
import usonia.foundation.Room
import usonia.foundation.Site

object ConfigurationAccessStub: ConfigurationAccess {
    override val parameters: Flow<ParameterBag> get() = emptyFlow()
    override val site: Flow<Site> get() = emptyFlow()
    override val rooms: Flow<Set<Room>> get() = emptyFlow()
    override val devices: Flow<Set<Device>> get() = emptyFlow()
}
