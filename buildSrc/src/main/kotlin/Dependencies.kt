object Coroutines: DependencyGroup(
    group = "org.jetbrains.kotlinx",
    version = "1.4.1"
) {
    val test = dependency("kotlinx-coroutines-test")
    val core = dependency("kotlinx-coroutines-core")
    val js = dependency("kotlinx-coroutines-core-js")
}

object JUnit {
    const val core = "junit:junit:4.12"
}

object Kimchi: DependencyGroup(
    group = "com.github.inkapplications.kimchi",
    version = "991aa06d88dd3"
) {
    val core = dependency("kimchi")
    val logger = dependency("logger")
}

object Ktor: DependencyGroup(
    group = "io.ktor",
    version = "1.4.1"
) {
    val client = dependency("ktor-client-core")
    val clientJson = dependency("ktor-client-json")
    val clientSerialization = dependency("ktor-client-serialization")
    val clientOkHttp = dependency("ktor-client-okhttp")
    val clientJs = dependency("ktor-client-js")
    val clientJsonJs = dependency("ktor-client-json-js")
    val serverNetty = dependency("ktor-server-netty")
    val websockets = dependency("ktor-websockets")
}

object KotlinX {
    const val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:0.1.0"
    const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1"
}
