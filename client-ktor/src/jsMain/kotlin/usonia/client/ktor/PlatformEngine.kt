package usonia.client.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual val PlatformEngine: HttpClientEngineFactory<*> = Js
