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
                api(libraries.coroutines.core)
                api(libraries.kotlinx.serialization.json)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libraries.bundles.sqldelight)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.coroutines.test)
                implementation(projects.foundationTesting)
                implementation(projects.coreTesting)
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
