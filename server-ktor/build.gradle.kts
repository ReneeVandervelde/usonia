plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_15
    targetCompatibility = org.gradle.api.JavaVersion.VERSION_15
}

dependencies {
    api(libs.kimchi.logger)
    api(project(":server"))
    api(libs.kotlin.coroutines.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.iospin.libsodium)
    implementation("org.slf4j:slf4j-nop:1.7.30")
}
