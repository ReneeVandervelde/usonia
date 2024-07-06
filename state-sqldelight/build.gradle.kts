plugins {
    backendlibrary()
    id("com.squareup.sqldelight")
}

sqldelight {
    database("Database") {
        packageName = "usonia.state"
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.core)
                api(projects.foundation)
                api(kotlinLibraries.coroutines.core)
                api(kotlinLibraries.serialization.json)
                implementation(inkLibraries.regolith.data)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(inkLibraries.regolith.data)
                implementation(libraries.bundles.sqldelight)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.coroutines.test)
                implementation(projects.foundationTesting)
                implementation(projects.coreTesting)
                implementation(kotlinLibraries.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
