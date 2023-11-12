package usonia.server.test

import regolith.init.InitTarget
import regolith.init.TargetManager
import kotlin.reflect.KClass

object DummyManager: TargetManager {
    override suspend fun <T : InitTarget> awaitTarget(targetClass: KClass<T>): T {
        TODO()
    }
    override suspend fun postTarget(target: InitTarget) {}
}
