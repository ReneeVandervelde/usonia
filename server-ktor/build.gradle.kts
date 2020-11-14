plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":server"))
    api(Kimchi.logger)
    implementation("io.ktor:ktor-server-netty:1.4.1")
    implementation("io.ktor:ktor-websockets:1.4.1")
    implementation("org.slf4j:slf4j-nop:1.7.30")
}
