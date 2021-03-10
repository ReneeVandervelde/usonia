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
                api(KotlinX.serialization)
                implementation(project(":hue-bridge"))
                implementation(project(":schlage"))
                implementation(project(":smartthings"))
                implementation(project(":xiaomi"))
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
