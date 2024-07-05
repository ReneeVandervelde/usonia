package usonia.server

import kotlinx.coroutines.CoroutineScope
import regolith.init.InitRunnerCallbacks
import regolith.init.Initializer
import regolith.init.RegolithInitRunner
import regolith.init.TargetManager

/**
 * Runs the plugin initializers as a subprocess that emits its own target.
 *
 * This is so that the initializers can be completed before any daemons are
 * started.
 */
internal class PluginInitializer(
    initializers: List<Initializer>,
    initCallbacks: InitRunnerCallbacks,
    initScope: CoroutineScope,
): Initializer {
    private val regolithRunner = RegolithInitRunner(
        initializers = initializers,
        callbacks = initCallbacks,
        initializerScope = initScope,
    )

    override suspend fun initialize(targetManager: TargetManager) {
        regolithRunner.initialize().join()
        targetManager.postTarget(PluginInitTarget)
    }
}
