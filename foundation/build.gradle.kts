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
                api("com.github.ajalt.colormath:colormath:2.0.0")
                api(project(":kotlin-extensions"))
                api(KotlinX.dateTime)
                api(Kimchi.logger)
                implementation(KotlinX.serialization)
                api("com.github.inkapplications.spondee:math:f1d6f9b")
                api("com.github.inkapplications.spondee:measures:f1d6f9b")
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
