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
                api(project(":core"))
                api(KotlinX.serialization)
                api(Coroutines.core)
                implementation(Ktor.client)
                implementation(Ktor.clientSerialization)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(Ktor.clientOkHttp)
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
                implementation(Ktor.clientJs)
                implementation(Ktor.clientJsonJs)
            }
        }
    }
}
