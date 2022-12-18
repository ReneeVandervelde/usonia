plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    implementation(kotlinLibraries.gradle)
    implementation(libraries.sqldelight.gradle)
    implementation(kotlinLibraries.serialization.gradle)
}
