plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlin-extensions"))
                api(project(":core"))
                implementation(project(":foundation"))
                api(project(":state"))
                api(Coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.github.inkapplications.Shade:shade:1.2.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(Coroutines.test)
                implementation(project(":state-testing"))
                implementation(project(":foundation-testing"))
            }
        }
    }
}
