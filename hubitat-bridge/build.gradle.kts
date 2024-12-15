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
                api(libs.kotlin.coroutines.core)
                implementation(projects.clientKtor)
                implementation(libs.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotlin.test.junit)
                implementation(projects.coreTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
