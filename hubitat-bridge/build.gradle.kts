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
                api(projects.foundation)
                api(projects.serialization)
                api(kotlinLibraries.coroutines.core)
                implementation(projects.clientKtor)
                implementation(ktorLibraries.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.coroutines.test)
                implementation(kotlinLibraries.test.junit)
                implementation(projects.coreTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
