
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Coroutines.core)
    implementation("io.ktor:ktor-server-netty:1.4.1")
    implementation("org.slf4j:slf4j-nop:1.7.30")
}

