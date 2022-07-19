package usonia.client.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual val PlatformEngine: HttpClientEngineFactory<*> = OkHttp
