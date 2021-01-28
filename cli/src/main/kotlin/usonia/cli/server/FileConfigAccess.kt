package usonia.cli.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Site
import usonia.kotlin.getResourceContents
import usonia.kotlin.suspendedFlow
import java.lang.UnsupportedOperationException

/**
 * Provide config access from a statically loaded resource.
 */
class FileConfigAccess(
    private val json: Json,
): ConfigurationAccess {
    val siteValue by lazy {
        getResourceContents("config.json")
            .let {
                json.decodeFromString(Site.serializer(), it)
            }
    }
    override val site: Flow<Site> = suspendedFlow(siteValue)
    override val flags: Flow<Map<String, String?>> = emptyFlow()

    override suspend fun updateSite(site: Site) = throw UnsupportedOperationException("Refusing to modify file contents")
    override suspend fun setFlag(key: String, value: String?) = throw UnsupportedOperationException("Cannot store settings to file.")
    override suspend fun removeFlag(key: String) = throw UnsupportedOperationException("Cannot store settings to file.")
}
