plugins {
    `kotlin-dsl`
}
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
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
