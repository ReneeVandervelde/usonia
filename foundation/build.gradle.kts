plugins {
    multiplatformLibrary()
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.github.ajalt.colormath:colormath:3.2.0")
                api(projects.kotlinExtensions)
                api(kotlinLibraries.datetime)
                api(inkLibraries.kimchi.logger)
                implementation(kotlinLibraries.serialization.json)
                api(inkLibraries.spondee)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlinLibraries.test.junit)
                implementation(libraries.junit)
            }
        }
    }
}
