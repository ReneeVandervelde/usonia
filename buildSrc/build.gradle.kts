plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    implementation(libraries.kotlin.gradle)
    implementation(libraries.sqldelight.gradle)
    implementation(libraries.kotlinx.serialization.gradle)
}
