plugins {
    kotlin("jvm")
}

dependencies {
    api(libraries.kimchi.logger)
    api(project(":server"))
    api(libraries.coroutines.core)
    implementation(libraries.ktor.server.netty)
    implementation(libraries.ktor.websockets)
    implementation("org.slf4j:slf4j-nop:1.7.30")
}
