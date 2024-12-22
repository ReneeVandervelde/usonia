plugins {
    `kotlin-dsl`
}
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(15)
        vendor = JvmVendorSpec.ADOPTOPENJDK
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
