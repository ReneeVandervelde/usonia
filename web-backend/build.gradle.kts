plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.core)
                api(projects.server)
                api(projects.serialization)
                api(libs.kimchi.logger)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}
