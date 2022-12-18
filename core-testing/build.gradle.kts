plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundationTesting)
                api(projects.core)
                api(kotlinLibraries.coroutines.core)
            }
        }
    }
}
