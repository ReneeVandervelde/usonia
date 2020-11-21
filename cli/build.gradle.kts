plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
}

application {
    applicationName = "usonia"
    mainClassName = "usonia.cli.MainKt"
}

dependencies {
    implementation(project(":app"))
    implementation(project(":client"))
    implementation(project(":core"))
    implementation(project(":server-ktor"))
    implementation(project(":state-memory"))
    implementation(Coroutines.core)
    implementation(Kimchi.core)
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    implementation("com.google.dagger:dagger:2.30")
    kapt("com.google.dagger:dagger-compiler:2.30")
}
