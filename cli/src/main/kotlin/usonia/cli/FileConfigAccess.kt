package usonia.cli

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import usonia.foundation.Site
import usonia.kotlin.getResourceContents
import usonia.serialization.SiteSerializer
import usonia.state.ConfigurationAccess

/**
 * Provide config access from a statically loaded resource.
 */
class FileConfigAccess: ConfigurationAccess {
    val siteValue by lazy {
        getResourceContents("config.json")
            .let {
                Json.decodeFromString(SiteSerializer, it)
            }
    }
    override val site: Flow<Site> = flow {
        emit(siteValue)
    }
}
