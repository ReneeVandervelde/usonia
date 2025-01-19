package com.inkapplications.telegram.client

import io.ktor.client.engine.*
import usonia.client.ktor.PlatformEngine

/**
 * Internal platform specific dependencies.
 */
object KtorPlatformModule {
    val engine: HttpClientEngineFactory<*> = PlatformEngine
}
