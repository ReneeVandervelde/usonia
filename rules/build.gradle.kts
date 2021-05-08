plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinExtensions)
                api(projects.core)
                api(projects.server)
                implementation(projects.foundation)
                implementation(projects.weather)
                api(libraries.coroutines.core)
                implementation(libraries.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
