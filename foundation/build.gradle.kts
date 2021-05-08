plugins {
    multiplatformLibrary()
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.github.ajalt.colormath:colormath:2.0.0")
                api(projects.kotlinExtensions)
                api(libraries.kotlinx.datetime)
                api(libraries.kimchi.logger)
                implementation(libraries.kotlinx.serialization.json)
                api("com.inkapplications.spondee:math:0.0.0")
                api("com.inkapplications.spondee:measures:0.0.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libraries.kotlin.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
