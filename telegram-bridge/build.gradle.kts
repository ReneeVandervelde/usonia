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
                implementation(projects.serialization)
                implementation(projects.rules)
                api(kotlinLibraries.coroutines.core)
                implementation(ktorLibraries.client.core)
                implementation("com.inkapplications.telegram:client:0.3.1")
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
