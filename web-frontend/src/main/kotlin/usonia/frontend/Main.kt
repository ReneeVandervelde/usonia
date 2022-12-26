package usonia.frontend

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import usonia.kotlin.awaitAll

fun main() {
    FrontendModule.jobScope.launch {
        FrontendModule.controllers.awaitAll {
            async {
                FrontendModule.logger.debug { "Running ${it::class.simpleName}" }
                it.onReady()
            }
        }
    }
}
