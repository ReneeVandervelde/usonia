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
                api(kotlinLibraries.coroutines.core)
                implementation(ktorLibraries.client.core)
                implementation(projects.clientKtor)
                implementation(ktorLibraries.client.contentnegotiation)
                implementation(ktorLibraries.serialization.json)
                implementation(ktorLibraries.client.serialization)
                implementation(kotlinLibraries.datetime)
                implementation(kotlinLibraries.serialization.json)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(kotlinLibraries.test.junit)
                implementation(kotlinLibraries.coroutines.test)
            }
        }
    }
}
