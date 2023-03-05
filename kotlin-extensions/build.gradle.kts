plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinLibraries.coroutines.core)
                api(kotlinLibraries.datetime)
                api(inkLibraries.bundles.watermelon.kotlin)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
