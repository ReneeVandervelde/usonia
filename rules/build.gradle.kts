plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.kotlinExtensions)
            api(projects.core)
            api(projects.server)
            implementation(projects.foundation)
            implementation(projects.weather)
            api(libs.kotlin.coroutines.core)
            implementation(libs.ktor.client.core)
            api(projects.celestials)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test.core)
            implementation(libs.kotlin.coroutines.test)
            implementation(projects.coreTesting)
            implementation(projects.serverTesting)
            implementation(projects.foundationTesting)
        }
    }
}
