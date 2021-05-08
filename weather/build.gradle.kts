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
                api(projects.foundation)
                api(libraries.coroutines.core)
                implementation(libraries.ktor.client.core)
                implementation(libraries.ktor.client.json)
                implementation(libraries.ktor.client.serialization)
                implementation(libraries.kotlinx.datetime)
                implementation(libraries.kotlinx.serialization.json)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.coroutines.test)
            }
        }
    }
}
