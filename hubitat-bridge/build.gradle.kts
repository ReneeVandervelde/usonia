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
                api(libraries.coroutines.core)
                implementation(projects.clientKtor)
                implementation(libraries.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.coroutines.test)
                implementation(libraries.kotlin.test.junit)
                implementation(projects.coreTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
