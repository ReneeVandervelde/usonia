package usonia.client.ktor

import io.ktor.client.engine.*

expect val PlatformEngine: HttpClientEngineFactory<*>
