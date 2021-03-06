plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                api(projects.core)
                api(libraries.kotlinx.serialization.json)
                api(libraries.coroutines.core)
                implementation(libraries.ktor.client.core)
                implementation(libraries.ktor.client.serialization)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libraries.ktor.client.okhttp)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.junit)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libraries.ktor.client.js.core)
                implementation(libraries.ktor.client.js.json)
            }
        }
    }
}
