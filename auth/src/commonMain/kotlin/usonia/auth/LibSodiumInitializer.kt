package usonia.auth

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import regolith.init.Initializer
import regolith.init.TargetManager

/**
 * Regolith Initializer Adapter for LibSodium.
 */
internal object LibSodiumInitializer: Initializer {
    override suspend fun initialize(targetManager: TargetManager) {
        if (!LibsodiumInitializer.isInitialized()) {
            LibsodiumInitializer.initialize()
        }
        targetManager.postTarget(AuthInit)
    }
}
