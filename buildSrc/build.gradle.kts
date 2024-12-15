plugins {
    `kotlin-dsl`
}
repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.sqldelight.gradle)
    implementation(libs.kotlin.serialization.gradle)
}
