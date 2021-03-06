plugins {
    backendlibrary()
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinExtensions)
                api(projects.core)
                api(projects.server)
                implementation(projects.foundation)
                api(libraries.coroutines.core)
                implementation(libraries.ktor.client.core)
                implementation(libraries.ktor.client.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.foundationTesting)
                implementation(projects.serverTesting)
            }
        }
    }
}
