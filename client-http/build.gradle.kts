plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.foundation)
                api(projects.core)
                api(kotlinLibraries.serialization.json)
                api(kotlinLibraries.coroutines.core)
                implementation(projects.clientKtor)
                implementation(ktorLibraries.client.core)
                implementation(ktorLibraries.client.websockets)
                implementation(ktorLibraries.client.contentnegotiation)
                implementation(ktorLibraries.client.serialization)
                implementation(ktorLibraries.serialization.json)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(ktorLibraries.client.okhttp)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.test.junit)
                implementation(libraries.junit)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(ktorLibraries.client.js.core)
                implementation(ktorLibraries.client.js.json)
            }
        }
    }
}
