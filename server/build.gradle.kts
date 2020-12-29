plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kotlin-extensions"))
                implementation(project(":foundation"))
                api(KotlinX.dateTime)
                api(KotlinX.serialization)
                api(Kimchi.logger)
            }
        }
    }
}
