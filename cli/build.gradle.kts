plugins {
    application
    kotlin("jvm")
}

application {
    applicationName = "usonia"
    mainClassName = "usonia.cli.MainKt"
}

dependencies {
    implementation(project(":client"))
    implementation(project(":core"))
    implementation(project(":server-ktor"))
    implementation(project(":state-memory"))
    implementation(Coroutines.core)
    implementation(Kimchi.core)
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
}
