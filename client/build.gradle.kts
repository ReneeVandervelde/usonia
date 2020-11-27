plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":foundation"))
                api(project(":state"))
                api(project(":serialization"))
                api(Coroutines.core)
                implementation(Ktor.client)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:1.4.1")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(JUnit.core)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:1.4.1")
            }
        }
    }
}
