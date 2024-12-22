plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(15)
        vendor = JvmVendorSpec.ADOPTOPENJDK
    }
}

dependencies {
    api(libs.kimchi.logger)
    api(project(":server"))
    api(libs.kotlin.coroutines.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation("org.slf4j:slf4j-nop:1.7.30")
}
