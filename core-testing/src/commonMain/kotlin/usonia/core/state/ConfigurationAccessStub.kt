package usonia.core.state

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import usonia.foundation.SecurityState
import usonia.foundation.Site

object ConfigurationAccessStub: ConfigurationAccess {
    override val site: OngoingFlow<Site> get() = ongoingFlowOf()
    override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf()
    override val securityState: OngoingFlow<SecurityState> = ongoingFlowOf()
    override suspend fun updateSite(site: Site) = Unit
    override suspend fun setFlag(key: String, value: String?) = Unit
    override suspend fun removeFlag(key: String) = Unit
    override suspend fun armSecurity() = Unit
}
