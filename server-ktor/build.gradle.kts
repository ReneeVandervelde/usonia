plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_15
    targetCompatibility = org.gradle.api.JavaVersion.VERSION_15
}

dependencies {
    api(inkLibraries.kimchi.logger)
    api(project(":server"))
    api(kotlinLibraries.coroutines.core)
    implementation(ktorLibraries.server.netty)
    implementation(ktorLibraries.server.websockets)
    implementation("org.slf4j:slf4j-nop:1.7.30")
}
