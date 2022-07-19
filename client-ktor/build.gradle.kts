plugins {
    multiplatformLibrary()
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libraries.ktor.client.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libraries.ktor.client.okhttp)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libraries.ktor.client.js.core)
            }
        }
    }
}
