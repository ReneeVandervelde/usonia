plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                api(kotlinLibraries.coroutines.core)
            }
        }
    }
}
