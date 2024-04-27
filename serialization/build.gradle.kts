plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                api(kotlinLibraries.serialization.json)
                implementation(projects.hueBridge)
                implementation(projects.archetypes)
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
