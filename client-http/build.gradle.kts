plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                api(projects.core)
                api(libs.kotlin.serialization.json)
                api(libs.kotlin.coroutines.core)
                implementation(projects.clientKtor)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.client.contentnegotiation)
                implementation(libs.ktor.client.serialization)
                implementation(libs.ktor.serialization.json)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js.core)
                implementation(libs.ktor.client.js.json)
            }
        }
    }
}
