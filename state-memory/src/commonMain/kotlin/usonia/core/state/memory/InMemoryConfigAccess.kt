package usonia.core.state.memory

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import usonia.core.state.ConfigurationAccess
import usonia.foundation.SecurityState
import usonia.foundation.Site
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.first

/**
 * Stores configuration temporarily in memory.
 */
class InMemoryConfigAccess: ConfigurationAccess {
    private val mutableSite = MutableSharedFlow<Site>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val mutableFlags = MutableSharedFlow<Map<String, String?>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).also {
        it.tryEmit(emptyMap())
    }
    private val mutableSecurityState = MutableStateFlow(SecurityState.Disarmed)
    override val site = mutableSite.asOngoing()
    override val flags = mutableFlags.asOngoing()
    override val securityState: OngoingFlow<SecurityState> = mutableSecurityState.asOngoing()

    override suspend fun updateSite(site: Site) {
        this.mutableSite.emit(site)
    }

    override suspend fun setFlag(key: String, value: String?) {
        flags.first()
            .toMutableMap()
            .also { it[key] = value }
            .run { mutableFlags.emit(this) }
    }

    override suspend fun removeFlag(key: String) {
        flags.first()
            .toMutableMap()
            .also { it.remove(key) }
            .run { mutableFlags.emit(this) }
    }

    override suspend fun armSecurity() {
        mutableSecurityState.value = SecurityState.Armed
    }
}
