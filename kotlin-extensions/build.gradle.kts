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
                api(libraries.coroutines.core)
                api(libraries.kotlinx.datetime)
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
