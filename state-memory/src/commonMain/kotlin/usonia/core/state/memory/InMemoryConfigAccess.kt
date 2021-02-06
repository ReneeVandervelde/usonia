package usonia.core.state.memory

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Site

/**
 * Stores configuration temporarily in memory.
 */
class InMemoryConfigAccess: ConfigurationAccess {
    override val site = MutableSharedFlow<Site>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val flags = MutableSharedFlow<Map<String, String?>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).also {
        it.tryEmit(emptyMap())
    }

    override suspend fun updateSite(site: Site) {
        this.site.emit(site)
    }

    override suspend fun setFlag(key: String, value: String?) {
        flags.first()
            .toMutableMap()
            .also { it[key] = value }
            .run { flags.emit(this) }
    }

    override suspend fun removeFlag(key: String) {
        flags.first()
            .toMutableMap()
            .also { it.remove(key) }
            .run { flags.emit(this) }
    }
}
