plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                implementation(libs.iospin.libsodium)
                api(libs.regolith.processes)
            }
        }
    }
}
