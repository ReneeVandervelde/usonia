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
                implementation(projects.weather)
                api(libs.kotlin.coroutines.core)
                implementation(libs.ktor.client.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotlin.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
