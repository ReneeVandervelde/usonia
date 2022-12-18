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
                api(kotlinLibraries.coroutines.core)
                implementation(projects.clientKtor)
                implementation(ktorLibraries.client.contentnegotiation)
                implementation(ktorLibraries.serialization.json)
                implementation(ktorLibraries.client.core)
                implementation(ktorLibraries.client.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.test.junit)
                implementation(kotlinLibraries.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.foundationTesting)
                implementation(projects.serverTesting)
            }
        }
    }
}
