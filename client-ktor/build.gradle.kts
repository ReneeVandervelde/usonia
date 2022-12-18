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
                api(ktorLibraries.client.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(ktorLibraries.client.okhttp)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(ktorLibraries.client.js.core)
            }
        }
    }
}
