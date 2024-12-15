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
                api(libs.kotlin.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(projects.clientKtor)
                implementation(libs.ktor.client.contentnegotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.serialization)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotlin.coroutines.test)
            }
        }
    }
}
