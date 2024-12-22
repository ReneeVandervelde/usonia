plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kotlinExtensions)
                implementation(projects.foundation)
                api(projects.core)
                implementation(projects.auth)
                implementation(libs.iospin.libsodium)
                api(libs.kotlin.datetime)
                api(libs.kotlin.serialization.json)
                api(libs.kimchi.logger)
                api(libs.regolith.processes)
            }
        }
    }
}
