plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kotlinExtensions)
                implementation(projects.foundation)
                implementation(projects.core)
                api(kotlinLibraries.datetime)
                api(kotlinLibraries.serialization.json)
                api(inkLibraries.kimchi.logger)
            }
        }
    }
}
