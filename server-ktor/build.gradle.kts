plugins {
    kotlin("jvm")
}

dependencies {
    api(Kimchi.logger)
    api(project(":server"))
    api(Coroutines.core)
    implementation(Ktor.serverNetty)
    implementation(Ktor.websockets)
    implementation("org.slf4j:slf4j-nop:1.7.30")
}
