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
                api(libs.kotlin.coroutines.core)
                implementation(projects.clientKtor)
                implementation(libs.ktor.client.contentnegotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotlin.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.foundationTesting)
                implementation(projects.serverTesting)
            }
        }
    }
}
