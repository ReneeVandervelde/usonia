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
                implementation(projects.weather)
                implementation(projects.foundation)
                implementation(projects.rules)
                api(kotlinLibraries.coroutines.core)
                implementation(ktorLibraries.client.core)
                implementation(inkLibraries.glass.client)
                implementation(inkLibraries.regolith.timemachine)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.test.junit)
                implementation(kotlinLibraries.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
