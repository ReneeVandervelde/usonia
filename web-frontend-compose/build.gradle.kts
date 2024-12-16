plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(compose.runtime)
                implementation(libs.kotlin.coroutines.js)
                implementation(libs.kimchi.core)
                implementation(projects.kotlinExtensions)
                implementation(projects.clientHttp)
                implementation(projects.serialization)
                implementation(npm("chart.js", "4.2.0"))
            }
        }
    }
}
