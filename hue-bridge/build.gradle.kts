plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinExtensions)
                implementation(projects.foundation)
                api(kotlinLibraries.coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(projects.core)
                api(projects.server)
                implementation("com.inkapplications.shade:core:2.0.0-alpha")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(kotlinLibraries.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
