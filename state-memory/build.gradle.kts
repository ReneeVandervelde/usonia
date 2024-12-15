plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.core)
                api(projects.foundation)
                api(libs.kotlin.coroutines.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
                implementation(projects.foundationTesting)
                implementation(projects.coreTesting)
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }
    }
}
