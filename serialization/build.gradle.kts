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
                implementation(projects.schlage)
                implementation(projects.smartthings)
                implementation(projects.xiaomi)
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
