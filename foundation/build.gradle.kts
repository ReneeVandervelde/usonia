plugins {
    multiplatformLibrary()
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.github.ajalt.colormath:colormath:3.6.1")
                api(projects.kotlinExtensions)
                api(libs.kotlin.datetime)
                api(libs.kimchi.logger)
                implementation(libs.kotlin.serialization.json)
                api(libs.spondee)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
            }
        }
    }
}
