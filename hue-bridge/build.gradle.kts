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
                api(project(":kotlin-extensions"))
                implementation(project(":foundation"))
                api(Coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(project(":core"))
                api(project(":server"))
                implementation("com.github.inkapplications.Shade:shade:1.2.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(Coroutines.test)
                implementation(project(":core-testing"))
                implementation(project(":server-testing"))
                implementation(project(":foundation-testing"))
            }
        }
    }
}
