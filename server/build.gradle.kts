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
                api(libraries.kotlinx.datetime)
                api(libraries.kotlinx.serialization.json)
                api(libraries.kimchi.logger)
            }
        }
    }
}
