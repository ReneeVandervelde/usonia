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
                api(libs.kotlin.coroutines.core)
                api(libs.kotlin.serialization.json)
                implementation(libs.regolith.data)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.regolith.data)
                implementation(libs.bundles.sqldelight)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
                implementation(projects.foundationTesting)
                implementation(projects.coreTesting)
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }
    }
}
