plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                api(libraries.kotlinx.serialization.json)
                implementation(projects.hueBridge)
                implementation(projects.schlage)
                implementation(projects.smartthings)
                implementation(projects.xiaomi)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
