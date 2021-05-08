plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kotlinExtensions)
                api(projects.foundation)
            }
        }
    }
}
