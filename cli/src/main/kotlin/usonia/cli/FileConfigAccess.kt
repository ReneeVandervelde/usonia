package usonia.cli

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Site
import usonia.kotlin.getResourceContents
import usonia.kotlin.suspendedFlow
import usonia.serialization.SiteSerializer

/**
 * Provide config access from a statically loaded resource.
 */
class FileConfigAccess(
    private val siteSerializer: SiteSerializer,
): ConfigurationAccess {
    val siteValue by lazy {
        getResourceContents("config.json")
            .let {
                Json.decodeFromString(siteSerializer, it)
            }
    }
    override val site: Flow<Site> = suspendedFlow(siteValue)
}
