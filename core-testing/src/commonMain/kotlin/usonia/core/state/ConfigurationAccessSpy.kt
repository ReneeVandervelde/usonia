package usonia.core.state

import usonia.foundation.Site

open class ConfigurationAccessSpy: ConfigurationAccess by ConfigurationAccessStub {
    val siteUpdates = mutableListOf<Site>()
    val flagUpdates = mutableListOf<Pair<String, String?>>()
    val removedFlags = mutableListOf<String>()

    override suspend fun updateSite(site: Site) {
        siteUpdates += site
    }

    override suspend fun setFlag(key: String, value: String?) {
        flagUpdates += key to value
    }

    override suspend fun removeFlag(key: String) {
        removedFlags += key
    }
}
