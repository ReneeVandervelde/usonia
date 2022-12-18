plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.coreTesting)
                api(projects.server)
                api(kotlinLibraries.coroutines.core)
            }
        }
    }
}
