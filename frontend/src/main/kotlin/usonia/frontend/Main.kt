package usonia.frontend

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import usonia.kotlin.awaitAll

fun main() {
    GlobalScope.launch {
        FrontendModule.controllers.awaitAll {
            async {
                FrontendModule.logger.debug { "Binding ${it::class.simpleName}" }
                it.bind()
            }
        }
    }
}
