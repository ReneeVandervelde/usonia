plugins {
    multiplatformLibrary()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinExtensions)
                implementation(projects.foundation)
                api(libraries.coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(projects.core)
                api(projects.server)
                implementation("com.github.inkapplications.Shade:shade:1.2.0")
                implementation("org.jetbrains.kotlinx:atomicfu-common") {
                    version {
                        strictly("0.13.1")
                    }
                }
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libraries.coroutines.test)
                implementation(projects.coreTesting)
                implementation(projects.serverTesting)
                implementation(projects.foundationTesting)
            }
        }
    }
}
