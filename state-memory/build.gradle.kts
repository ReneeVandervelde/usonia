plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.core)
                api(projects.foundation)
                api(libraries.coroutines.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.coroutines.test)
                implementation(projects.foundationTesting)
                implementation(projects.coreTesting)
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
