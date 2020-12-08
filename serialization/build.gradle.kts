plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.10"
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
                api(Coroutines.core)
                api(KotlinX.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(JUnit.core)
            }
        }
    }
}