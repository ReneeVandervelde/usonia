package usonia.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import usonia.foundation.Site

object ConfigurationAccessStub: ConfigurationAccess {
    override val site: Flow<Site> get() = emptyFlow()
}
