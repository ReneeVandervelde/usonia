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
                api(Coroutines.core)
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
