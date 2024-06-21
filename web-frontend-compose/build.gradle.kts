plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.6.10"
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
                implementation(kotlinLibraries.coroutines.js)
                implementation(inkLibraries.kimchi.core)
                implementation(projects.kotlinExtensions)
                implementation(projects.clientHttp)
                implementation(projects.serialization)
                implementation(npm("chart.js", "4.2.0"))
            }
        }
    }
}
