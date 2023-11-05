plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinExtensions)
                api(kotlinLibraries.datetime)
                api(kotlinLibraries.coroutines.core)
            }
        }
    }
}
